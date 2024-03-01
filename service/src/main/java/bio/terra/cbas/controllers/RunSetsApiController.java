package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MethodUtil.convertToMethodSourceEnum;
import static bio.terra.cbas.common.MetricsUtil.recordInputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordOutputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRecordsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRunsSubmittedPerRunSet;
import static bio.terra.cbas.model.RunSetState.CANCELING;
import static bio.terra.cbas.model.RunSetState.ERROR;
import static bio.terra.cbas.model.RunSetState.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.INITIALIZING;
import static bio.terra.cbas.models.CbasRunStatus.QUEUED;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MethodUtil;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.common.exceptions.MethodProcessingException.UnknownMethodSourceException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wds.WdsClientUtils;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.cbas.dependencies.wds.WdsServiceException;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.AbortRunSetResponse;
import bio.terra.cbas.model.OutputDestination;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.RunSetDetailsResponse;
import bio.terra.cbas.model.RunSetListResponse;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager.AbortRequestDetails;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import bio.terra.cbas.runsets.types.CoercionException;
import bio.terra.cbas.util.UuidSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import cromwell.client.model.WorkflowIdAndStatus;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;

@Controller
public class RunSetsApiController implements RunSetsApi {

  private final SamService samService;
  private final CromwellService cromwellService;
  private final WdsService wdsService;
  private final DockstoreService dockstoreService;
  private final MethodVersionDao methodVersionDao;
  private final MethodDao methodDao;
  private final RunSetDao runSetDao;
  private final RunDao runDao;
  private final ObjectMapper objectMapper;
  private final CbasApiConfiguration cbasApiConfiguration;
  private final CbasContextConfiguration cbasContextConfiguration;
  private final SmartRunSetsPoller smartRunSetsPoller;
  private final UuidSource uuidSource;
  private final RunSetAbortManager abortManager;

  private record WdsRecordResponseDetails(
      ArrayList<RecordResponse> recordResponseList, Map<String, String> recordIdsWithError) {}

  public RunSetsApiController(
      SamService samService,
      CromwellService cromwellService,
      WdsService wdsService,
      DockstoreService dockstoreService,
      ObjectMapper objectMapper,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      RunDao runDao,
      RunSetDao runSetDao,
      CbasApiConfiguration cbasApiConfiguration,
      CbasContextConfiguration cbasContextConfiguration,
      SmartRunSetsPoller smartRunSetsPoller,
      UuidSource uuidSource,
      RunSetAbortManager abortManager) {
    this.samService = samService;
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
    this.dockstoreService = dockstoreService;
    this.objectMapper = objectMapper;
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.cbasApiConfiguration = cbasApiConfiguration;
    this.cbasContextConfiguration = cbasContextConfiguration;
    this.smartRunSetsPoller = smartRunSetsPoller;
    this.uuidSource = uuidSource;
    this.abortManager = abortManager;
  }

  private RunSetDetailsResponse convertToRunSetDetails(RunSet runSet) {
    return new RunSetDetailsResponse()
        .runSetId(runSet.runSetId())
        .methodId(runSet.methodVersion().method().methodId())
        .methodVersionId(runSet.methodVersion().methodVersionId())
        .runSetName(runSet.name())
        .runSetDescription(runSet.description())
        .callCachingEnabled(runSet.callCachingEnabled())
        .isTemplate(runSet.isTemplate())
        .state(runSet.status().toCbasRunSetApiState())
        .recordType(runSet.recordType())
        .submissionTimestamp(DateUtils.convertToDate(runSet.submissionTimestamp()))
        .lastModifiedTimestamp(DateUtils.convertToDate(runSet.lastModifiedTimestamp()))
        .runCount(runSet.runCount())
        .errorCount(runSet.errorCount())
        .inputDefinition(runSet.inputDefinition())
        .outputDefinition(runSet.outputDefinition())
        .userId(runSet.userId());
  }

  @Override
  public ResponseEntity<RunSetListResponse> getRunSets(UUID methodId, Integer pageSize) {
    if (!samService.hasReadPermission()) {
      throw new ForbiddenException(SamService.READ_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    RunSetListResponse response;

    List<RunSet> filteredRunSet;

    if (methodId != null) {
      filteredRunSet = Collections.singletonList(runSetDao.getRunSetWithMethodId(methodId));
    } else {
      filteredRunSet = runSetDao.getRunSets(pageSize, false);
    }

    TimeLimitedUpdater.UpdateResult<RunSet> runSetUpdateResult =
        smartRunSetsPoller.updateRunSets(filteredRunSet);
    List<RunSet> updatedRunSets = runSetUpdateResult.updatedList();
    List<RunSetDetailsResponse> filteredRunSetDetails =
        updatedRunSets.stream().map(this::convertToRunSetDetails).toList();
    response =
        new RunSetListResponse()
            .runSets(filteredRunSetDetails)
            .fullyUpdated(runSetUpdateResult.fullyUpdated());

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  public RunSet preRegisterRunSet(MethodVersion methodVersion,
                                  String runSetName,
                                  String runSetDescription,
                                  Boolean callCachingEnabled,
                                  List<WorkflowInputDefinition> workflowInputDefinitions,
                                  List<WorkflowOutputDefinition> workflowOutputDefinitions,
                                  String recordType,
                                  String userSubjectId


  ) throws JsonProcessingException {

    UUID runSetId = this.uuidSource.generateUUID();

    RunSet runSet = new RunSet(
              runSetId,
              methodVersion,
              runSetName,
              runSetDescription,
              callCachingEnabled,
              false,
              CbasRunSetStatus.QUEUED,
              DateUtils.currentTimeInUTC(),
              DateUtils.currentTimeInUTC(),
              DateUtils.currentTimeInUTC(),
              0,
              0,
              objectMapper.writeValueAsString(workflowInputDefinitions),
              objectMapper.writeValueAsString(workflowOutputDefinitions),
              recordType,
              userSubjectId,
              cbasContextConfiguration.getWorkspaceId());

    runSetDao.createRunSet(runSet);
    methodDao.updateLastRunWithRunSet(runSet);
    methodVersionDao.updateLastRunWithRunSet(runSet);

    return runSet;
  }

  public List<RunStateResponse> preRegisterRuns(RunSet runSet, Map<String, UUID> dataTableIdToRunIdMapping) throws DatabaseConnectivityException.RunCreationException {

    List<RunStateResponse> responses = new ArrayList<>();

    for (Map.Entry<String, UUID> entry : dataTableIdToRunIdMapping.entrySet()) {
      int createdRows = runDao.createRun(
          new Run(
              entry.getValue(),
              entry.getValue().toString(),
              runSet,
              entry.getKey(),
              DateUtils.currentTimeInUTC(),
              CbasRunStatus.QUEUED,
              DateUtils.currentTimeInUTC(),
              DateUtils.currentTimeInUTC(),
              null)
      );
      if (createdRows != 1) {
        throw new DatabaseConnectivityException.RunCreationException(entry.getValue(), entry.getKey());
      }
      responses.add(new RunStateResponse().runId(entry.getValue()).state(CbasRunStatus.toCbasApiState(QUEUED)));
    }
    return responses;
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {

    captureRequestMetrics(request);

    if (!samService.hasWritePermission()) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    // request validation
    List<String> requestErrors = validateRequest(request, this.cbasApiConfiguration);
    if (!requestErrors.isEmpty()) {
      String errorMsg = "Bad user request. Error(s): " + requestErrors;
      log.warn(errorMsg);
      return new ResponseEntity<>(
          new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST);
    }

    UserStatusInfo user = samService.getSamUser();

    MethodVersion methodVersion = methodVersionDao.getMethodVersion(request.getMethodVersionId());

    RunSet runSet;
    try {
      runSet = preRegisterRunSet(
          methodVersion,
          request.getRunSetName(),
          request.getRunSetDescription(),
          request.isCallCachingEnabled(),
          request.getWorkflowInputDefinitions(),
          request.getWorkflowOutputDefinitions(),
          request.getWdsRecords().getRecordType(),
          user.getUserSubjectId()
      );
    } catch (JsonProcessingException e) {
      log.warn("Failed to record run set to database", e);
      return new ResponseEntity<>(
          new RunSetStateResponse()
              .errors("Failed to record run set to database. Error(s): " + e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
    UUID runSetId = runSet.runSetId();

    // Create mapping between record IDs and run IDs:
    Map<String, UUID> dataTableIdToRunIdMapping =
        request.getWdsRecords().getRecordIds().stream()
            .map(recordId -> Map.entry(recordId, uuidSource.generateUUID()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    List<RunStateResponse> runStateResponseList;
    try {
      runStateResponseList = preRegisterRuns(runSet, dataTableIdToRunIdMapping);
    } catch (DatabaseConnectivityException.RunCreationException e) {
      log.error("Failed to record runs to database", e);
      runSetDao.updateStateAndRunDetails(
          runSetId,
          CbasRunSetStatus.ERROR,
          0,
          0,
          OffsetDateTime.now()
      );
      return new ResponseEntity<>(
          new RunSetStateResponse()
              .errors("Failed to record runs to database. Error(s): " + e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    RunSetStateResponse response =
        new RunSetStateResponse().runSetId(runSetId).runs(runStateResponseList).state(runSet.status().toCbasRunSetApiState());

    captureResponseMetrics(response);

    triggerWorkflowSubmit(request, methodVersion, runSet, dataTableIdToRunIdMapping, runSetId);

    // Return the result
    return new ResponseEntity<>(response, HttpStatus.OK);

  }

  @Async("runSetExecutor")
  protected void triggerWorkflowSubmit(RunSetRequest request, MethodVersion methodVersion, RunSet runSet, Map<String, UUID> dataTableIdToRunIdMapping, UUID runSetId) {
    // Fetch WDS Records and keep track of errors while retrieving records
    WdsRecordResponseDetails wdsRecordResponses = fetchWdsRecords(request);

    if (!wdsRecordResponses.recordIdsWithError.isEmpty()) {
      String errorMsg =
          "Error while fetching WDS Records for Record ID(s): "
              + wdsRecordResponses.recordIdsWithError;
      log.warn(errorMsg);
      runSetDao.updateStateAndRunDetails(
          runSetId,
          CbasRunSetStatus.ERROR,
          0,
          0,
          OffsetDateTime.now()
      );
    }

    // convert method url to raw url and use that while calling Cromwell's submit workflow
    // endpoint
    String rawMethodUrl;
    try {
      PostMethodRequest.MethodSourceEnum methodSourceEnum =
          convertToMethodSourceEnum(methodVersion.method().methodSource());

      rawMethodUrl =
          MethodUtil.convertToRawUrl(
              methodVersion.url(), methodSourceEnum, methodVersion.name(), dockstoreService);

      // this could happen if there was no url or empty url received in the Dockstore workflow's
      // descriptor response
      if (rawMethodUrl == null || rawMethodUrl.isEmpty()) {
        String errorMsg =
            "Error while retrieving WDL url for Dockstore workflow. No workflow url found specified path.";
        log.warn(errorMsg);
        runSetDao.updateStateAndRunDetails(
            runSetId,
            CbasRunSetStatus.ERROR,
            0,
            0,
            OffsetDateTime.now()
        );
        return;
      }
    } catch (URISyntaxException
        | MalformedURLException
        | UnknownMethodSourceException
        | bio.terra.dockstore.client.ApiException e) {
      // the flow shouldn't reach here since if it was invalid URL or invalid method source it
      // should have been caught when method was imported
      String errorMsg =
          "Something went wrong while submitting workflow. Error: %s".formatted(e.getMessage());
      log.error(errorMsg, e);
      runSetDao.updateStateAndRunDetails(
          runSetId,
          CbasRunSetStatus.ERROR,
          0,
          0,
          OffsetDateTime.now()
      );
      return;
    }


    // For each Record ID, build workflow inputs and submit the workflow to Cromwell
    List<RunStateResponse> runStateResponseList =
        buildInputsAndSubmitRun(
            request, runSet, wdsRecordResponses.recordResponseList, rawMethodUrl, dataTableIdToRunIdMapping);

    // Figure out how many runs are in Failed state. If all Runs are in an Error state then mark
    // the Run Set as Failed
    RunSetState runSetState;
    List<RunStateResponse> runsInErrorState =
        runStateResponseList.stream()
            .filter(run -> CbasRunStatus.fromValue(run.getState()).inErrorState())
            .toList();

    if (runsInErrorState.size() == request.getWdsRecords().getRecordIds().size()) {
      runSetState = ERROR;
    } else runSetState = RUNNING;

    runSetDao.updateStateAndRunDetails(
        runSetId,
        CbasRunSetStatus.fromValue(runSetState),
        runStateResponseList.size(),
        runsInErrorState.size(),
        OffsetDateTime.now());
  }

  @Override
  public ResponseEntity<AbortRunSetResponse> abortRunSet(UUID runSetId) {
    if (!samService.hasWritePermission()) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    AbortRunSetResponse aborted = new AbortRunSetResponse();

    aborted.runSetId(runSetId);

    AbortRequestDetails abortDetails = abortManager.abortRunSet(runSetId);
    List<String> failedRunIds = abortDetails.getAbortRequestFailedIds();
    List<UUID> submittedAbortWorkflows = abortDetails.getAbortRequestSubmittedIds();

    if (!failedRunIds.isEmpty()) {
      aborted.errors(
          "Run set canceled with errors. Unable to abort workflow(s): %s".formatted(failedRunIds));
    }

    aborted.state(CANCELING);
    aborted.runs(submittedAbortWorkflows);

    return new ResponseEntity<>(aborted, HttpStatus.OK);
  }

  public static void captureRequestMetrics(RunSetRequest request) {
    recordInputsInRequest(request.getWorkflowInputDefinitions().size());
    recordOutputsInRequest(request.getWorkflowOutputDefinitions().size());
    recordRecordsInRequest(request.getWdsRecords().getRecordIds().size());
  }

  public static void captureResponseMetrics(RunSetStateResponse response) {
    long successfulRuns =
        response.getRuns().stream().filter(r -> r.getState() == RunState.UNKNOWN).count();
    recordRunsSubmittedPerRunSet(successfulRuns);
  }

  public static List<String> validateRequest(RunSetRequest request, CbasApiConfiguration config) {
    List<String> errorList = new ArrayList<>();
    errorList.addAll(validateRequestRecordIds(request, config));
    errorList.addAll(validateRequestInputsAndOutputs(request, config));
    return errorList;
  }

  public static List<String> validateRequestRecordIds(
      RunSetRequest request, CbasApiConfiguration config) {
    List<String> errorList = new ArrayList<>();

    // check number of Record IDs in request is within allowed limit
    int recordIdsSize = request.getWdsRecords().getRecordIds().size();
    int recordIdsMax = config.getRunSetsMaximumRecordIds();
    if (recordIdsSize > recordIdsMax) {
      errorList.add(
          "%s record IDs submitted exceeds the maximum value of %s."
              .formatted(recordIdsSize, recordIdsMax));
    }

    // check that there are no duplicated Record IDs present in the request
    List<String> recordIds = request.getWdsRecords().getRecordIds();
    List<String> duplicateRecordIds =
        recordIds.stream().filter(e -> Collections.frequency(recordIds, e) > 1).distinct().toList();
    if (duplicateRecordIds.size() > 0) {
      errorList.add("Duplicate Record ID(s) %s present in request.".formatted(duplicateRecordIds));
    }
    return errorList;
  }

  public static List<String> validateRequestInputsAndOutputs(
      RunSetRequest request, CbasApiConfiguration config) {
    List<String> errorList = new ArrayList<>();

    // check that the number of outputs does not exceed their maximum allowed values
    int numWorkflowInputs = request.getWorkflowInputDefinitions().size();
    int maxWorkflowInputs = config.getMaxWorkflowInputs();
    if (numWorkflowInputs > maxWorkflowInputs) {
      errorList.add(
          "Number of defined inputs (%s) exceeds maximum value (%s)"
              .formatted(numWorkflowInputs, maxWorkflowInputs));
    }

    // check that the number of outputs does not exceed their maximum allowed values
    long numWorkflowOutputs =
        request.getWorkflowOutputDefinitions().stream()
            .filter(
                output ->
                    output.getDestination().getType() == OutputDestination.TypeEnum.RECORD_UPDATE)
            .count();
    int maxWorkflowOutputs = config.getMaxWorkflowOutputs();
    if (numWorkflowOutputs > maxWorkflowOutputs) {
      errorList.add(
          "Number of defined outputs (%s) exceeds maximum value (%s)"
              .formatted(numWorkflowOutputs, maxWorkflowOutputs));
    }

    return errorList;
  }

  private WdsRecordResponseDetails fetchWdsRecords(RunSetRequest request) {
    String recordType = request.getWdsRecords().getRecordType();

    ArrayList<RecordResponse> recordResponses = new ArrayList<>();
    HashMap<String, String> recordIdsWithError = new HashMap<>();
    for (String recordId : request.getWdsRecords().getRecordIds()) {
      try {
        recordResponses.add(wdsService.getRecord(recordType, recordId));
      } catch (WdsServiceApiException e) {
        log.warn("Record lookup for Record ID {} failed.", recordId, e);
        recordIdsWithError.put(recordId, WdsClientUtils.extractErrorMessage(e.getMessage()));
      } catch (WdsServiceException e) {
        log.warn("Record lookup for Record ID {} failed.", recordId, e);
        recordIdsWithError.put(recordId, e.getMessage());
      }
    }

    return new WdsRecordResponseDetails(recordResponses, recordIdsWithError);
  }

  private RunStateResponse recordFailureToStartRun(UUID runId, String error) {
    runDao.updateRunStatusWithError(runId, SYSTEM_ERROR, DateUtils.currentTimeInUTC(), error);
    return new RunStateResponse()
        .runId(runId)
        .state(CbasRunStatus.toCbasApiState(SYSTEM_ERROR))
        .errors(error);
  }

  private RunStateResponse recordSuccessInitializingRun(UUID runId) {
    runDao.updateRunStatus(runId, INITIALIZING, DateUtils.currentTimeInUTC());
    return new RunStateResponse()
        .runId(runId)
        .state(CbasRunStatus.toCbasApiState(INITIALIZING))
        .errors(null);
  }

  private List<RunStateResponse> buildInputsAndSubmitRun(
      RunSetRequest request,
      RunSet runSet,
      ArrayList<RecordResponse> recordResponses,
      String rawMethodUrl,
      Map<String, UUID> recordIdToRunIdMapping) {
    ArrayList<RunStateResponse> runStateResponseList = new ArrayList<>();

    // Build the JSON that specifies additional configuration for cromwell workflows. The same
    // options
    // will be used for all workflows submitted as part of this run set.
    String workflowOptionsJson =
        cromwellService.buildWorkflowOptionsJson(
            Objects.requireNonNullElse(runSet.callCachingEnabled(), true));

    for (List<RecordResponse> batch :
        Lists.partition(recordResponses, cbasApiConfiguration.getMaxWorkflowsInBatch())) {

      Map<UUID, RecordResponse> requestedIdToRecord =
          batch.stream()
              .map(singleRecord -> Map.entry(recordIdToRunIdMapping.get(singleRecord.getId()), singleRecord))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // Build the inputs set from workflow parameter definitions and the fetched record
      Map<UUID, String> requestedIdToWorkflowInput =
          requestedIdToRecord.entrySet().stream()
              .map(
                  entry -> {
                    UUID runId = entry.getKey();

                    try {
                      return Map.entry(
                          entry.getKey(),
                          InputGenerator.inputsToJson(
                              InputGenerator.buildInputs(
                                  request.getWorkflowInputDefinitions(), entry.getValue())));
                    } catch (CoercionException e) {
                      String errorMsg =
                          String.format(
                              "Input generation failed for record %s. Coercion error: %s",
                              entry.getValue().getId(), e.getMessage());
                      log.warn(errorMsg, e);
                      runStateResponseList.add(
                          recordFailureToStartRun(runId, errorMsg));
                    } catch (InputProcessingException e) {
                      log.warn(e.getMessage());
                      runStateResponseList.add(
                          recordFailureToStartRun(runId, e.getMessage()));
                    } catch (JsonProcessingException e) {
                      // Should be super rare that jackson cannot convert an object to Json...
                      String errorMsg =
                          String.format(
                              "Failed to convert inputs object to JSON for batch in RunSet %s.",
                              runSet.runSetId());
                      log.warn(errorMsg, e);
                      runStateResponseList.add(
                          recordFailureToStartRun(runId, errorMsg + e.getMessage()));
                    }
                    return null;
                  })
              .filter(inputs -> !Objects.isNull(inputs))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      if (requestedIdToWorkflowInput.isEmpty()) {
        return runStateResponseList;
      }

      try {
        // Submit the workflows and store the Runs to database
        List<WorkflowIdAndStatus> submitWorkflowBatchResponse =
            cromwellService.submitWorkflowBatch(
                rawMethodUrl, requestedIdToWorkflowInput, workflowOptionsJson);

        runStateResponseList.addAll(
            submitWorkflowBatchResponse.stream()
                .map(
                    idAndStatus -> {
                      UUID requestedId = UUID.fromString(idAndStatus.getId());
                      return recordSuccessInitializingRun(requestedId);
                    })
                .toList());
      } catch (cromwell.client.ApiException e) {
        String errorMsg =
            String.format(
                "Cromwell submission failed for batch in RunSet %s. ApiException: ",
                runSet.runSetId());
        log.warn(errorMsg, e);
        runStateResponseList.addAll(
            requestedIdToWorkflowInput.keySet().stream()
                .map(
                    requestedId ->
                        recordFailureToStartRun(
                            requestedId, errorMsg + e.getMessage()))
                .toList());
      }
    }

    return runStateResponseList;
  }
}
