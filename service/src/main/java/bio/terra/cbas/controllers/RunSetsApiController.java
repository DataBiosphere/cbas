package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MetricsUtil.recordInputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordOutputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRecordsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRunsSubmittedPerRunSet;
import static bio.terra.cbas.model.RunSetState.CANCELING;
import static bio.terra.cbas.models.CbasRunStatus.QUEUED;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.controllers.helper.RunSetSubmissionHelper;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.AbortRunSetResponse;
import bio.terra.cbas.model.OutputDestination;
import bio.terra.cbas.model.RunSetDetailsResponse;
import bio.terra.cbas.model.RunSetListResponse;
import bio.terra.cbas.model.RunSetRequest;
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
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager.AbortRequestDetails;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import bio.terra.cbas.util.UuidSource;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
  private final BearerTokenFactory bearerTokenFactory;
  private final HttpServletRequest httpServletRequest;
  private final RunSetSubmissionHelper runSetSubmissionHelper;

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
      RunSetAbortManager abortManager,
      BearerTokenFactory bearerTokenFactory,
      HttpServletRequest httpServletRequest,
      RunSetSubmissionHelper runSetSubmissionHelper) {
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
    this.bearerTokenFactory = bearerTokenFactory;
    this.httpServletRequest = httpServletRequest;
    this.runSetSubmissionHelper = runSetSubmissionHelper;
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
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    if (!samService.hasReadPermission(userToken)) {
      throw new ForbiddenException(SamService.READ_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    RunSetListResponse response;

    List<RunSet> filteredRunSet;

    if (methodId != null) {
      filteredRunSet = Collections.singletonList(runSetDao.getLatestRunSetWithMethodId(methodId));
    } else {
      filteredRunSet = runSetDao.getRunSets(pageSize, false);
    }

    TimeLimitedUpdater.UpdateResult<RunSet> runSetUpdateResult =
        smartRunSetsPoller.updateRunSets(filteredRunSet, userToken);
    List<RunSet> updatedRunSets = runSetUpdateResult.updatedList();
    List<RunSetDetailsResponse> filteredRunSetDetails =
        updatedRunSets.stream().map(this::convertToRunSetDetails).toList();
    response =
        new RunSetListResponse()
            .runSets(filteredRunSetDetails)
            .fullyUpdated(runSetUpdateResult.fullyUpdated());

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  public RunSet preRegisterRunSet(
      MethodVersion methodVersion,
      String runSetName,
      String runSetDescription,
      Boolean callCachingEnabled,
      List<WorkflowInputDefinition> workflowInputDefinitions,
      List<WorkflowOutputDefinition> workflowOutputDefinitions,
      String recordType,
      String userSubjectId)
      throws JsonProcessingException {

    UUID runSetId = this.uuidSource.generateUUID();

    log.info("### FIND ME - pre-registering run set %s".formatted(runSetId));

    RunSet runSet =
        new RunSet(
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

  public List<RunStateResponse> preRegisterRuns(
      RunSet runSet, Map<String, UUID> dataTableIdToRunIdMapping)
      throws DatabaseConnectivityException.RunCreationException {

    log.info(
        "### FIND ME - pre-registering runs for run set %s"
            .formatted(runSet.runSetId().toString()));

    List<RunStateResponse> responses = new ArrayList<>();

    for (Map.Entry<String, UUID> entry : dataTableIdToRunIdMapping.entrySet()) {
      int createdRows =
          runDao.createRun(
              new Run(
                  entry.getValue(),
                  entry.getValue().toString(),
                  runSet,
                  entry.getKey(),
                  DateUtils.currentTimeInUTC(),
                  CbasRunStatus.QUEUED,
                  DateUtils.currentTimeInUTC(),
                  DateUtils.currentTimeInUTC(),
                  null));
      if (createdRows != 1) {
        throw new DatabaseConnectivityException.RunCreationException(
            entry.getValue(), entry.getKey());
      }
      responses.add(
          new RunStateResponse()
              .runId(entry.getValue())
              .state(CbasRunStatus.toCbasApiState(QUEUED)));
    }
    return responses;
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {
    long requestReceivedTime = System.currentTimeMillis();

    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    captureRequestMetrics(request);

    long samCheckStartTime = System.currentTimeMillis();
    if (!samService.hasWritePermission(userToken)) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }
    long samCheckEndTime = System.currentTimeMillis();

    // request validation
    List<String> requestErrors = validateRequest(request, this.cbasApiConfiguration);
    if (!requestErrors.isEmpty()) {
      String errorMsg = "Bad user request. Error(s): " + requestErrors;
      log.warn(errorMsg);
      return new ResponseEntity<>(
          new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST);
    }

    long getSamUserStartTime = System.currentTimeMillis();
    UserStatusInfo user = samService.getSamUser(userToken);
    long getSamUserEndTime = System.currentTimeMillis();

    MethodVersion methodVersion = methodVersionDao.getMethodVersion(request.getMethodVersionId());

    RunSet runSet;
    try {
      runSet =
          preRegisterRunSet(
              methodVersion,
              request.getRunSetName(),
              request.getRunSetDescription(),
              request.isCallCachingEnabled(),
              request.getWorkflowInputDefinitions(),
              request.getWorkflowOutputDefinitions(),
              request.getWdsRecords().getRecordType(),
              user.getUserSubjectId());
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
          runSetId, CbasRunSetStatus.ERROR, 0, 0, OffsetDateTime.now());
      return new ResponseEntity<>(
          new RunSetStateResponse()
              .errors("Failed to record runs to database. Error(s): " + e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    RunSetStateResponse response =
        new RunSetStateResponse()
            .runSetId(runSetId)
            .runs(runStateResponseList)
            .state(runSet.status().toCbasRunSetApiState());

    captureResponseMetrics(response);

    log.info("### FIND ME About to trigger workflow submit for RunSet {}", runSetId);
    runSetSubmissionHelper.triggerWorkflowSubmit(
        wdsService,
        cromwellService,
        dockstoreService,
        request,
        methodVersion,
        runSet,
        dataTableIdToRunIdMapping,
        runSetId,
        userToken);

    long requestEndTime = System.currentTimeMillis();

    // Print timings
    log.info(
        "### FIND ME - Timings from postRunSet() run set %s ### Check permissions with SAM: %s ### Get User info from SAM: %s ### Total request timing: %s"
            .formatted(
                runSetId,
                samCheckEndTime - samCheckStartTime,
                getSamUserEndTime - getSamUserStartTime,
                requestEndTime - requestReceivedTime));

    // Return the result
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<AbortRunSetResponse> abortRunSet(UUID runSetId) {
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    if (!samService.hasWritePermission(userToken)) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    AbortRunSetResponse aborted = new AbortRunSetResponse();

    aborted.runSetId(runSetId);

    AbortRequestDetails abortDetails = abortManager.abortRunSet(runSetId, userToken);
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
}
