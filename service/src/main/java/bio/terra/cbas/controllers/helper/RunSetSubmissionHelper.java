package bio.terra.cbas.controllers.helper;

import static bio.terra.cbas.common.MethodUtil.convertToMethodSourceEnum;
import static bio.terra.cbas.model.RunSetState.ERROR;
import static bio.terra.cbas.model.RunSetState.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.INITIALIZING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MethodUtil;
import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.wds.WdsClientUtils;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.cbas.dependencies.wds.WdsServiceException;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import bio.terra.cbas.runsets.types.CoercionException;
import bio.terra.common.iam.BearerToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import cromwell.client.model.WorkflowIdAndStatus;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RunSetSubmissionHelper {

  private final RunDao runDao;
  private final RunSetDao runSetDao;
  private final CbasApiConfiguration cbasApiConfiguration;

  private final Logger logger = LoggerFactory.getLogger(RunSetSubmissionHelper.class);

  public RunSetSubmissionHelper(
      RunSetDao runSetDao, CbasApiConfiguration cbasApiConfiguration, RunDao runDao) {
    this.runSetDao = runSetDao;
    this.cbasApiConfiguration = cbasApiConfiguration;
    this.runDao = runDao;
  }

  private record WdsRecordResponseDetails(
      ArrayList<RecordResponse> recordResponseList, Map<String, String> recordIdsWithError) {}

  @Async("runSetExecutor")
  public void triggerWorkflowSubmit(
      WdsService wdsService,
      CromwellService cromwellService,
      DockstoreService dockstoreService,
      RunSetRequest request,
      MethodVersion methodVersion,
      RunSet runSet,
      Map<String, UUID> dataTableIdToRunIdMapping,
      UUID runSetId,
      BearerToken userToken) {

    logger.info("Triggering workflow submit for RunSet {}", runSetId);

    // Fetch WDS Records and keep track of errors while retrieving records
    WdsRecordResponseDetails wdsRecordResponses = fetchWdsRecords(wdsService, request, userToken);

    if (!wdsRecordResponses.recordIdsWithError.isEmpty()) {
      String errorMsg =
          "Error while fetching WDS Records for Record ID(s): "
              + wdsRecordResponses.recordIdsWithError;
      logger.warn(errorMsg);
      runSetDao.updateStateAndRunDetails(
          runSetId, CbasRunSetStatus.ERROR, 0, 0, OffsetDateTime.now());
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
        logger.warn(errorMsg);
        runSetDao.updateStateAndRunDetails(
            runSetId, CbasRunSetStatus.ERROR, 0, 0, OffsetDateTime.now());
        return;
      }
    } catch (URISyntaxException
        | MalformedURLException
        | MethodProcessingException.UnknownMethodSourceException
        | bio.terra.dockstore.client.ApiException e) {
      // the flow shouldn't reach here since if it was invalid URL or invalid method source it
      // should have been caught when method was imported
      String errorMsg =
          "Something went wrong while submitting workflow. Error: %s".formatted(e.getMessage());
      logger.error(errorMsg, e);
      runSetDao.updateStateAndRunDetails(
          runSetId, CbasRunSetStatus.ERROR, 0, 0, OffsetDateTime.now());
      return;
    }

    // For each Record ID, build workflow inputs and submit the workflow to Cromwell
    List<RunStateResponse> runStateResponseList =
        buildInputsAndSubmitRun(
            cromwellService,
            request,
            runSet,
            wdsRecordResponses.recordResponseList,
            rawMethodUrl,
            dataTableIdToRunIdMapping,
            userToken);

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

    logger.info("### FIND ME - triggerWorkflowSubmit complete for run set %s".formatted(runSetId));
  }

  private WdsRecordResponseDetails fetchWdsRecords(
      WdsService wdsService, RunSetRequest request, BearerToken userToken) {
    logger.info(
        "### FIND ME - fetching WDS records for run_set name %s"
            .formatted(request.getRunSetName()));

    String recordType = request.getWdsRecords().getRecordType();

    ArrayList<RecordResponse> recordResponses = new ArrayList<>();
    HashMap<String, String> recordIdsWithError = new HashMap<>();
    for (String recordId : request.getWdsRecords().getRecordIds()) {
      try {
        recordResponses.add(wdsService.getRecord(recordType, recordId, userToken));
      } catch (WdsServiceApiException e) {
        logger.warn("Record lookup for Record ID {} failed.", recordId, e);
        recordIdsWithError.put(recordId, WdsClientUtils.extractErrorMessage(e.getMessage()));
      } catch (WdsServiceException e) {
        logger.warn("Record lookup for Record ID {} failed.", recordId, e);
        recordIdsWithError.put(recordId, e.getMessage());
      }
    }

    return new WdsRecordResponseDetails(recordResponses, recordIdsWithError);
  }

  private List<RunStateResponse> buildInputsAndSubmitRun(
      CromwellService cromwellService,
      RunSetRequest request,
      RunSet runSet,
      ArrayList<RecordResponse> recordResponses,
      String rawMethodUrl,
      Map<String, UUID> recordIdToRunIdMapping,
      BearerToken userToken) {
    ArrayList<RunStateResponse> runStateResponseList = new ArrayList<>();

    logger.info(
        "### FIND ME - submitting workflows to Cromwell for run set %s"
            .formatted(runSet.runSetId().toString()));

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
              .map(
                  singleRecord ->
                      Map.entry(recordIdToRunIdMapping.get(singleRecord.getId()), singleRecord))
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
                      logger.warn(errorMsg, e);
                      runStateResponseList.add(recordFailureToStartRun(runId, errorMsg));
                    } catch (InputProcessingException e) {
                      logger.warn(e.getMessage());
                      runStateResponseList.add(recordFailureToStartRun(runId, e.getMessage()));
                    } catch (JsonProcessingException e) {
                      // Should be super rare that jackson cannot convert an object to Json...
                      String errorMsg =
                          String.format(
                              "Failed to convert inputs object to JSON for batch in RunSet %s.",
                              runSet.runSetId());
                      logger.warn(errorMsg, e);
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
                rawMethodUrl, requestedIdToWorkflowInput, workflowOptionsJson, userToken);

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
        logger.warn(errorMsg, e);
        runStateResponseList.addAll(
            requestedIdToWorkflowInput.keySet().stream()
                .map(requestedId -> recordFailureToStartRun(requestedId, errorMsg + e.getMessage()))
                .toList());
      }
    }

    return runStateResponseList;
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
}
