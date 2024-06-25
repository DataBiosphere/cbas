package bio.terra.cbas.service;

import static bio.terra.cbas.model.RunSetState.ERROR;
import static bio.terra.cbas.model.RunSetState.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.INITIALIZING;
import static bio.terra.cbas.models.CbasRunStatus.QUEUED;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunCreationException;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunSetCreationException;
import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.bard.BardService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsService.WdsRecordResponseDetails;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.models.SubmitRunResponse;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import bio.terra.cbas.runsets.types.CoercionException;
import bio.terra.cbas.util.UuidSource;
import bio.terra.common.iam.BearerToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import cromwell.client.model.WorkflowIdAndStatus;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RunSetsService {

  private final RunDao runDao;
  private final RunSetDao runSetDao;
  private final MethodDao methodDao;
  private final MethodVersionDao methodVersionDao;
  private final CromwellService cromwellService;
  private final WdsService wdsService;
  private final CbasApiConfiguration cbasApiConfiguration;
  private final UuidSource uuidSource;
  private final ObjectMapper objectMapper;
  private final CbasContextConfiguration cbasContextConfiguration;
  private final MicrometerMetrics micrometerMetrics;
  private final BardService bardService;

  private final Logger logger = LoggerFactory.getLogger(RunSetsService.class);

  public RunSetsService(
      RunDao runDao,
      RunSetDao runSetDao,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      CromwellService cromwellService,
      WdsService wdsService,
      CbasApiConfiguration cbasApiConfiguration,
      UuidSource uuidSource,
      ObjectMapper objectMapper,
      CbasContextConfiguration cbasContextConfiguration,
      MicrometerMetrics micrometerMetrics,
      BardService bardService) {
    this.runDao = runDao;
    this.runSetDao = runSetDao;
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
    this.cbasApiConfiguration = cbasApiConfiguration;
    this.uuidSource = uuidSource;
    this.objectMapper = objectMapper;
    this.cbasContextConfiguration = cbasContextConfiguration;
    this.micrometerMetrics = micrometerMetrics;
    this.bardService = bardService;
  }

  private record RunAndRecordDetails(UUID runId, RecordResponse recordResponse) {}

  public RunSet registerRunSet(
      RunSetRequest runSetRequest, UserStatusInfo user, MethodVersion methodVersion)
      throws JsonProcessingException, RunSetCreationException {
    UUID runSetId = uuidSource.generateUUID();

    RunSet newRunSet =
        new RunSet(
            runSetId,
            methodVersion,
            runSetRequest.getRunSetName(),
            runSetRequest.getRunSetDescription(),
            runSetRequest.isCallCachingEnabled(),
            false,
            CbasRunSetStatus.QUEUED,
            DateUtils.currentTimeInUTC(),
            DateUtils.currentTimeInUTC(),
            DateUtils.currentTimeInUTC(),
            0,
            0,
            objectMapper.writeValueAsString(runSetRequest.getWorkflowInputDefinitions()),
            objectMapper.writeValueAsString(runSetRequest.getWorkflowOutputDefinitions()),
            runSetRequest.getWdsRecords().getRecordType(),
            user.getUserSubjectId(),
            cbasContextConfiguration.getWorkspaceId());

    int created = runSetDao.createRunSet(newRunSet);

    if (created != 1) {
      throw new RunSetCreationException(runSetRequest.getRunSetName());
    }

    methodDao.updateLastRunWithRunSet(newRunSet);
    methodVersionDao.updateLastRunWithRunSet(newRunSet);

    return newRunSet;
  }

  public List<RunStateResponse> registerRunsInRunSet(
      RunSet runSet, Map<String, UUID> recordIdToRunIdMapping) throws RunCreationException {
    List<RunStateResponse> runStateResponseList = new ArrayList<>();

    for (Map.Entry<String, UUID> entry : recordIdToRunIdMapping.entrySet()) {
      int created =
          runDao.createRun(
              new Run(
                  entry.getValue(),
                  null,
                  runSet,
                  entry.getKey(),
                  DateUtils.currentTimeInUTC(),
                  QUEUED,
                  DateUtils.currentTimeInUTC(),
                  DateUtils.currentTimeInUTC(),
                  null));
      if (created != 1) {
        String errorMsg =
            "Failed to record runs to database for RunSet %s".formatted(runSet.runSetId());
        recordRunsAndRunSetInErrorState(runSet.runSetId(), errorMsg);

        throw new RunCreationException(runSet.runSetId(), entry.getValue(), entry.getKey());
      }

      runStateResponseList.add(
          new RunStateResponse()
              .runId(entry.getValue())
              .state(CbasRunStatus.toCbasApiState(QUEUED))
              .errors(""));
    }

    // update number of Runs in Run Set
    runSetDao.updateStateAndRunSetDetails(
        runSet.runSetId(), runSet.status(), runStateResponseList.size(), 0, OffsetDateTime.now());

    return runStateResponseList;
  }

  // Note: the order of params is important for async exception handler. If they are changed please
  // update handleExceptionFromAsyncSubmission() in AsyncExceptionHandler accordingly
  @Async
  public void triggerWorkflowSubmission(
      RunSetRequest request,
      RunSet runSet,
      Map<String, UUID> recordIdToRunIdMapping,
      BearerToken userToken,
      String rawMethodUrl,
      MethodVersion methodVersion,
      Timer.Sample requestTimerSample) {
    // Fetch WDS Records and keep track of errors while retrieving records
    WdsRecordResponseDetails wdsRecordResponses =
        fetchWdsRecords(wdsService, request, runSet, userToken);

    if (!wdsRecordResponses.recordIdsWithError().isEmpty()) {
      String errorMsg =
          "Error while fetching WDS Records for Record ID(s): "
              + wdsRecordResponses.recordIdsWithError();
      logger.warn(errorMsg);

      recordRunsAndRunSetInErrorState(runSet.runSetId(), errorMsg);

      return;
    }

    // For each Record ID, build workflow inputs and submit the workflow to Cromwell
    SubmitRunResponse runStateResponse =
        buildInputsAndSubmitRunSet(
            cromwellService,
            request,
            runSet,
            wdsRecordResponses.recordResponseList(),
            rawMethodUrl,
            recordIdToRunIdMapping,
            requestTimerSample,
            userToken);
    List<RunStateResponse> runStateResponseList = runStateResponse.runStateResponseList();

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

    runSetDao.updateStateAndRunSetDetails(
        runSet.runSetId(),
        CbasRunSetStatus.fromValue(runSetState),
        runStateResponseList.size(),
        runsInErrorState.size(),
        OffsetDateTime.now());
    logRunSetEvent(
        request, methodVersion, runStateResponse.successfullyInitializedWorkflowIds(), userToken);
  }

  private WdsRecordResponseDetails fetchWdsRecords(
      WdsService wdsService, RunSetRequest request, RunSet runSet, BearerToken userToken) {
    String recordType = request.getWdsRecords().getRecordType();

    WdsRecordResponseDetails responseDetails = wdsService.getRecords(recordType, request.getWdsRecords().getRecordIds(), userToken);
    Map<String, String> recordIdsWithError = responseDetails.recordIdsWithError();
    Timer.Sample wdsFetchRecordsSample = micrometerMetrics.startTimer();
    micrometerMetrics.stopTimer(
        wdsFetchRecordsSample,
        "wds_fetch_records_timer",
        "run_set_id",
        runSet.runSetId().toString(),
        "failed_record_requests",
        "%s".formatted(recordIdsWithError.size()),
        "failure_rate",
        "%s"
            .formatted(
                (double) recordIdsWithError.size()
                    / request.getWdsRecords().getRecordIds().size()));

    return responseDetails;

  }

  private RunStateResponse recordFailureToStartRun(UUID runId, String error) {
    runDao.updateRunStatusWithError(runId, SYSTEM_ERROR, DateUtils.currentTimeInUTC(), error);
    return new RunStateResponse()
        .runId(runId)
        .state(CbasRunStatus.toCbasApiState(SYSTEM_ERROR))
        .errors(error);
  }

  private RunStateResponse recordSuccessInitializingRun(UUID runId, UUID engineId) {
    runDao.updateEngineIdAndRunStatus(runId, engineId, INITIALIZING, DateUtils.currentTimeInUTC());
    return new RunStateResponse()
        .runId(runId)
        .state(CbasRunStatus.toCbasApiState(INITIALIZING))
        .errors(null);
  }

  private void recordRunsAndRunSetInErrorState(UUID runSetId, String errorMsg) {
    // before marking RunSet in Error state, ensure that any Runs that were created in
    // database also get marked as in Error state
    List<Run> runsInRunSet = runDao.getRuns(new RunDao.RunsFilters(runSetId, List.of(QUEUED)));
    runsInRunSet.forEach(
        run ->
            runDao.updateRunStatusWithError(
                run.runId(), CbasRunStatus.SYSTEM_ERROR, DateUtils.currentTimeInUTC(), errorMsg));

    // mark RunSet in Error state
    int runsCount = runsInRunSet.size();
    runSetDao.updateStateAndRunSetDetails(
        runSetId, CbasRunSetStatus.ERROR, runsCount, runsCount, OffsetDateTime.now());
  }

  private SubmitRunResponse buildInputsAndSubmitRunSet(
      CromwellService cromwellService,
      RunSetRequest request,
      RunSet runSet,
      List<RecordResponse> recordResponses,
      String rawMethodUrl,
      Map<String, UUID> recordIdToRunIdMapping,
      Timer.Sample cromwellRequestTimerSample,
      BearerToken userToken) {
    ArrayList<RunStateResponse> runStateResponseList = new ArrayList<>();
    ArrayList<String> successfullyInitializedWorkflowIds = new ArrayList<>();
    // Build the JSON that specifies additional configuration for cromwell workflows. The same
    // options will be used for all workflows submitted as part of this run set.
    String workflowOptionsJson =
        cromwellService.buildWorkflowOptionsJson(
            Objects.requireNonNullElse(runSet.callCachingEnabled(), true));

    Timer.Sample cromwellSubmitRunsSample = micrometerMetrics.startTimer();
    List<List<RecordResponse>> batches =
        Lists.partition(recordResponses, cbasApiConfiguration.getMaxWorkflowsInBatch());
    for (int batchIdx = 0; batchIdx < batches.size(); batchIdx += 1) {
      List<RecordResponse> batch = batches.get(batchIdx);
      // create a mapping from Engine ID -> class RunAndRecordDetails[Run ID, Record Response]
      Map<UUID, RunAndRecordDetails> engineIdToRunAndRecordMapping =
          batch.stream()
              .map(
                  singleRecord ->
                      Map.entry(
                          uuidSource.generateUUID(),
                          new RunAndRecordDetails(
                              recordIdToRunIdMapping.get(singleRecord.getId()), singleRecord)))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // Build the inputs set from workflow parameter definitions and the fetched record
      Map<UUID, String> engineIdToWorkflowInput =
          engineIdToRunAndRecordMapping.entrySet().stream()
              .map(
                  entry -> {
                    try {
                      return Map.entry(
                          entry.getKey(),
                          InputGenerator.inputsToJson(
                              InputGenerator.buildInputs(
                                  request.getWorkflowInputDefinitions(),
                                  entry.getValue().recordResponse)));
                    } catch (CoercionException e) {
                      String errorMsg =
                          String.format(
                              "Input generation failed for record %s. Coercion error: %s",
                              entry.getValue().recordResponse.getId(), e.getMessage());
                      logger.warn(errorMsg, e);
                      runStateResponseList.add(
                          recordFailureToStartRun(entry.getValue().runId, errorMsg));
                    } catch (InputProcessingException e) {
                      logger.warn(e.getMessage());
                      runStateResponseList.add(
                          recordFailureToStartRun(entry.getValue().runId, e.getMessage()));
                    } catch (JsonProcessingException e) {
                      // Should be super rare that jackson cannot convert an object to Json...
                      String errorMsg =
                          String.format(
                              "Failed to convert inputs object to JSON for batch in RunSet %s.",
                              runSet.runSetId());
                      logger.warn(errorMsg, e);
                      runStateResponseList.add(
                          recordFailureToStartRun(entry.getValue().runId, errorMsg));
                    }
                    return null;
                  })
              .filter(inputs -> !Objects.isNull(inputs))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      if (engineIdToWorkflowInput.isEmpty()) {
        return new SubmitRunResponse(runStateResponseList, List.of());
      }

      try {
        // Submit the workflows and store the Runs to database
        List<WorkflowIdAndStatus> submitWorkflowBatchResponse =
            cromwellService.submitWorkflowBatch(
                rawMethodUrl, engineIdToWorkflowInput, workflowOptionsJson, userToken);

        if (batchIdx == 0) {
          // record the time between the initial POST run set request
          // and the successful submission of the *first* batch of workflows in the run set.
          micrometerMetrics.stopTimer(
              cromwellRequestTimerSample,
              "cromwell_request_to_initial_submission_timer",
              "run_set_id",
              runSet.runSetId().toString());
        }
        if (batchIdx == batches.size() - 1) {
          // record the time between the initial POST run set request
          // and the successful submission of the *last* batch of workflows in the run set.
          micrometerMetrics.stopTimer(
              cromwellRequestTimerSample,
              "cromwell_request_to_final_submission_timer",
              "run_set_id",
              runSet.runSetId().toString());
        }

        runStateResponseList.addAll(
            submitWorkflowBatchResponse.stream()
                .map(
                    idAndStatus -> {
                      UUID engineId = UUID.fromString(idAndStatus.getId());
                      successfullyInitializedWorkflowIds.add(engineId.toString());
                      return recordSuccessInitializingRun(
                          engineIdToRunAndRecordMapping.get(engineId).runId, engineId);
                    })
                .toList());
      } catch (cromwell.client.ApiException e) {
        String errorMsg =
            String.format(
                "Cromwell submission failed for batch in RunSet %s. ApiException: ",
                runSet.runSetId());
        logger.warn(errorMsg, e);
        runStateResponseList.addAll(
            engineIdToWorkflowInput.keySet().stream()
                .map(
                    engineId ->
                        recordFailureToStartRun(
                            engineIdToRunAndRecordMapping.get(engineId).runId,
                            errorMsg + e.getMessage()))
                .toList());
      }
    }

    micrometerMetrics.stopTimer(
        cromwellSubmitRunsSample,
        "cromwell_submit_runs_timer",
        "run_set_id",
        runSet.runSetId().toString(),
        "runStateResponseList",
        runStateResponseList.toString());
    return new SubmitRunResponse(runStateResponseList, successfullyInitializedWorkflowIds);
  }

  public HashMap<String, String> getRunSetEventProperties(
      RunSetRequest request, MethodVersion methodVersion, List<String> workflowIds) {
    HashMap<String, String> properties = new HashMap<>();
    properties.put("runSetName", request.getRunSetName());
    properties.put("methodName", methodVersion.method().name());
    properties.put("methodSource", methodVersion.method().methodSource());
    properties.put("methodVersionName", methodVersion.name());
    properties.put("methodVersionUrl", methodVersion.url());
    properties.put("recordCount", String.valueOf(request.getWdsRecords().getRecordIds().size()));
    properties.put("workflowIds", workflowIds.toString());

    Optional<GithubMethodDetails> maybeGitHubMethodDetails =
        methodVersion.method().githubMethodDetails();
    if (maybeGitHubMethodDetails.isPresent()) {
      GithubMethodDetails githubMethodDetails = maybeGitHubMethodDetails.get();
      properties.put("githubOrganization", githubMethodDetails.organization());
      properties.put("githubRepository", githubMethodDetails.repository());
      properties.put("githubIsPrivate", githubMethodDetails.isPrivate().toString());
    }
    return properties;
  }

  public void logRunSetEvent(
      RunSetRequest request,
      MethodVersion methodVersion,
      List<String> workflowIds,
      BearerToken userToken) {
    String eventName = "workflow-submission";
    HashMap<String, String> properties =
        getRunSetEventProperties(request, methodVersion, workflowIds);
    bardService.logEvent(eventName, properties, userToken);
  }
}
