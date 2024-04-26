package bio.terra.cbas.controllers.util;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsClientUtils;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.cbas.dependencies.wds.WdsServiceException;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import bio.terra.cbas.runsets.types.CoercionException;
import bio.terra.common.iam.BearerToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import cromwell.client.model.WorkflowIdAndStatus;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static bio.terra.cbas.models.CbasRunStatus.INITIALIZING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;

public class RunSetsHelper {

  private final RunSetDao runSetDao;

  private final Logger logger = LoggerFactory.getLogger(RunSetsHelper.class);

  public RunSetsHelper(RunSetDao runSetDao) {
    this.runSetDao = runSetDao;
  }

  private record WdsRecordResponseDetails(
      ArrayList<RecordResponse> recordResponseList, Map<String, String> recordIdsWithError) {}

  private WdsRecordResponseDetails fetchWdsRecords(WdsService wdsService, RunSetRequest request, BearerToken userToken) {
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
      BearerToken userToken) {
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
              .map(singleRecord -> Map.entry(uuidSource.generateUUID(), singleRecord))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      Map<UUID, UUID> requestedIdToRunId =
          requestedIdToRecord.keySet().stream()
              .map(requestedId -> Map.entry(requestedId, uuidSource.generateUUID()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // Build the inputs set from workflow parameter definitions and the fetched record
      Map<UUID, String> requestedIdToWorkflowInput =
          requestedIdToRecord.entrySet().stream()
              .map(
                  entry -> {
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
                          storeRun(
                              requestedIdToRunId.get(entry.getKey()),
                              null,
                              runSet,
                              entry.getValue().getId(),
                              SYSTEM_ERROR,
                              errorMsg));
                    } catch (InputProcessingException e) {
                      log.warn(e.getMessage());
                      runStateResponseList.add(
                          storeRun(
                              requestedIdToRunId.get(entry.getKey()),
                              null,
                              runSet,
                              entry.getValue().getId(),
                              SYSTEM_ERROR,
                              e.getMessage()));
                    } catch (JsonProcessingException e) {
                      // Should be super rare that jackson cannot convert an object to Json...
                      String errorMsg =
                          String.format(
                              "Failed to convert inputs object to JSON for batch in RunSet %s.",
                              runSet.runSetId());
                      log.warn(errorMsg, e);
                      runStateResponseList.add(
                          storeRun(
                              requestedIdToRunId.get(entry.getKey()),
                              null,
                              runSet,
                              entry.getValue().getId(),
                              SYSTEM_ERROR,
                              errorMsg + e.getMessage()));
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
                      return storeRun(
                          requestedIdToRunId.get(requestedId),
                          idAndStatus.getId(),
                          runSet,
                          requestedIdToRecord.get(requestedId).getId(),
                          INITIALIZING,
                          null);
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
                        storeRun(
                            requestedIdToRunId.get(requestedId),
                            null,
                            runSet,
                            requestedIdToRecord.get(requestedId).getId(),
                            SYSTEM_ERROR,
                            errorMsg + e.getMessage()))
                .toList());
      }
    }

    return runStateResponseList;
  }

@Async("runSetExecutor")
  public void triggerWorkflowSubmission(WdsService wdsService, RunSetRequest request, RunSet runSet, BearerToken userToken, String rawMethodUrl) {

    // Fetch WDS Records and keep track of errors while retrieving records
    WdsRecordResponseDetails wdsRecordResponses = fetchWdsRecords(wdsService, request, userToken);

    if (wdsRecordResponses.recordIdsWithError.size() > 0) {
      String errorMsg =
          "Error while fetching WDS Records for Record ID(s): "
              + wdsRecordResponses.recordIdsWithError;
      logger.warn(errorMsg);

      int runsCount = request.getWdsRecords().getRecordIds().size();
      runSetDao.updateStateAndRunDetails(
          runSet.runSetId(), CbasRunSetStatus.ERROR, runsCount, runsCount, OffsetDateTime.now());

//      return new ResponseEntity<>(
//          new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST); ???????????

      // if we are here it means there might be Runs in DB equal to size of recordIds and they would also need to be updated Error state
      // or does SmartPoller handles it?
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

  }

}
