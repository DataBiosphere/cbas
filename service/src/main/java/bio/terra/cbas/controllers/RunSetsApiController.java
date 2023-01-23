package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MetricsUtil.recordInputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordOutputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRecordsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRunsSubmittedPerRunSet;
import static bio.terra.cbas.model.RunSetState.ERROR;
import static bio.terra.cbas.model.RunSetState.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.exceptions.WorkflowAttributesNotFoundException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetDetailsResponse;
import bio.terra.cbas.model.RunSetListResponse;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import cromwell.client.model.RunId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.ErrorResponse;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunSetsApiController implements RunSetsApi {

  private final CromwellService cromwellService;
  private final WdsService wdsService;
  private final MethodVersionDao methodVersionDao;
  private final MethodDao methodDao;
  private final RunSetDao runSetDao;
  private final RunDao runDao;
  private final ObjectMapper objectMapper;
  private final CbasApiConfiguration cbasApiConfiguration;
  private final SmartRunSetsPoller smartRunSetsPoller;

  private record WdsRecordResponseDetails(
      ArrayList<RecordResponse> recordResponseList, Map<String, String> recordIdsWithError) {}

  public RunSetsApiController(
      CromwellService cromwellService,
      WdsService wdsService,
      ObjectMapper objectMapper,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      RunDao runDao,
      RunSetDao runSetDao,
      CbasApiConfiguration cbasApiConfiguration,
      SmartRunSetsPoller smartRunSetsPoller) {
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
    this.objectMapper = objectMapper;
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.cbasApiConfiguration = cbasApiConfiguration;
    this.smartRunSetsPoller = smartRunSetsPoller;
  }

  private RunSetDetailsResponse convertToRunSetDetails(RunSet runSet) {
    return new RunSetDetailsResponse()
        .runSetId(runSet.runSetId())
        .methodId(runSet.methodVersion().method().method_id())
        .methodVersionId(runSet.methodVersion().methodVersionId())
        .runSetName(runSet.name())
        .runSetDescription(runSet.description())
        .isTemplate(runSet.isTemplate())
        .state(CbasRunSetStatus.toCbasRunSetApiState(runSet.status()))
        .recordType(runSet.recordType())
        .submissionTimestamp(DateUtils.convertToDate(runSet.submissionTimestamp()))
        .lastModifiedTimestamp(DateUtils.convertToDate(runSet.lastModifiedTimestamp()))
        .runCount(runSet.runCount())
        .errorCount(runSet.errorCount())
        .inputDefinition(runSet.inputDefinition())
        .outputDefinition(runSet.outputDefinition());
  }

  @Override
  public ResponseEntity<RunSetListResponse> getRunSets(UUID methodId, Integer pageSize) {
    List<RunSet> updatedRunSets;
    RunSetListResponse response;

    if (methodId != null) {
      List<RunSet> filteredRunSet =
          Collections.singletonList(runSetDao.getRunSetWithMethodId(methodId));
      updatedRunSets = smartRunSetsPoller.updateRunSets(filteredRunSet);
      List<RunSetDetailsResponse> filteredRunSetDetails =
          updatedRunSets.stream().map(this::convertToRunSetDetails).toList();
      response = new RunSetListResponse().runSets(filteredRunSetDetails);
    } else {
      List<RunSet> runSets = runSetDao.getRunSets();
      updatedRunSets = smartRunSetsPoller.updateRunSets(runSets);
      List<RunSetDetailsResponse> runSetDetails =
          updatedRunSets.stream().map(this::convertToRunSetDetails).toList();
      response = new RunSetListResponse().runSets(runSetDetails);
    }
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {
    captureRequestMetrics(request);

    // request validation
    List<String> requestErrors = validateRequest(request, this.cbasApiConfiguration);
    if (!requestErrors.isEmpty()) {
      String errorMsg = "Bad user request. Error(s): " + requestErrors;
      log.warn(errorMsg);
      return new ResponseEntity<>(
          new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST);
    }

    // Fetch WDS Records and keep track of errors while retrieving records
    WdsRecordResponseDetails wdsRecordResponses = fetchWdsRecords(request);
    if (wdsRecordResponses.recordIdsWithError.size() > 0) {
      String errorMsg =
          "Error while fetching WDS Records for Record ID(s): "
              + wdsRecordResponses.recordIdsWithError;
      log.warn(errorMsg);
      return new ResponseEntity<>(
          new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST);
    }

    // Fetch existing method:
    MethodVersion methodVersion = methodVersionDao.getMethodVersion(request.getMethodVersionId());

    // Create a new run_set
    UUID runSetId = UUID.randomUUID();
    RunSet runSet;

    try {
      runSet =
          new RunSet(
              runSetId,
              methodVersion,
              request.getRunSetName(),
              request.getRunSetDescription(),
              false,
              CbasRunSetStatus.UNKNOWN,
              DateUtils.currentTimeInUTC(),
              DateUtils.currentTimeInUTC(),
              DateUtils.currentTimeInUTC(),
              0,
              0,
              objectMapper.writeValueAsString(request.getWorkflowInputDefinitions()),
              objectMapper.writeValueAsString(request.getWorkflowOutputDefinitions()),
              request.getWdsRecords().getRecordType());
    } catch (JsonProcessingException e) {
      log.warn("Failed to record run set to database", e);
      return new ResponseEntity<>(
          new RunSetStateResponse()
              .errors("Failed to record run set to database. Error(s): " + e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
    runSetDao.createRunSet(runSet);

    methodDao.updateLastRunWithRunSet(runSet);
    methodVersionDao.updateLastRunWithRunSet(runSet);

    // For each Record ID, build workflow inputs and submit the workflow to Cromwell
    List<RunStateResponse> runStateResponseList =
        buildInputsAndSubmitRun(request, runSet, wdsRecordResponses.recordResponseList);

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
        runsInErrorState.size());

    RunSetStateResponse response =
        new RunSetStateResponse().runSetId(runSetId).runs(runStateResponseList).state(runSetState);

    captureResponseMetrics(response);

    // Return the result
    return new ResponseEntity<>(response, HttpStatus.OK);
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
    int numWorkflowOutputs = request.getWorkflowOutputDefinitions().size();
    int maxWorkflowOutputs = config.getMaxWorkflowOutputs();
    if (numWorkflowOutputs > maxWorkflowOutputs) {
      errorList.add(
          "Number of defined outputs (%s) exceeds maximum value (%s)"
              .formatted(numWorkflowOutputs, maxWorkflowOutputs));
    }

    return errorList;
  }

  private String getErrorMessage(ApiException exception) {
    Gson gson = new Gson();
    try {
      ErrorResponse error = gson.fromJson(exception.getResponseBody(), ErrorResponse.class);
      if (error != null) {
        return error.getMessage();
      } else {
        return exception.getMessage();
      }
    } catch (Exception e) {
      return exception.getMessage();
    }
  }

  private WdsRecordResponseDetails fetchWdsRecords(RunSetRequest request) {
    String recordType = request.getWdsRecords().getRecordType();

    ArrayList<RecordResponse> recordResponses = new ArrayList<>();
    HashMap<String, String> recordIdsWithError = new HashMap<>();
    for (String recordId : request.getWdsRecords().getRecordIds()) {
      try {
        recordResponses.add(wdsService.getRecord(recordType, recordId));
      } catch (ApiException e) {
        log.warn("Record lookup for Record ID {} failed.", recordId, e);
        recordIdsWithError.put(recordId, getErrorMessage(e));
      }
    }

    return new WdsRecordResponseDetails(recordResponses, recordIdsWithError);
  }

  private RunStateResponse storeRun(
      UUID runId,
      String externalId,
      RunSet runSet,
      String recordId,
      CbasRunStatus runState,
      String errors) {
    String additionalErrorMsg = "";
    int created =
        runDao.createRun(
            new Run(
                runId,
                externalId,
                runSet,
                recordId,
                DateUtils.currentTimeInUTC(),
                runState,
                DateUtils.currentTimeInUTC(),
                DateUtils.currentTimeInUTC(),
                errors));

    if (created != 1) {
      additionalErrorMsg =
          String.format(
              "CBAS failed to create new row for Record ID %s in %s state in database. INSERT returned '%s rows created'",
              recordId, runState, created);
      log.error(additionalErrorMsg);
    }

    return new RunStateResponse()
        .runId(runId)
        .state(CbasRunStatus.toCbasApiState(runState))
        .errors(errors + additionalErrorMsg);
  }

  private List<RunStateResponse> buildInputsAndSubmitRun(
      RunSetRequest request, RunSet runSet, ArrayList<RecordResponse> recordResponses) {
    ArrayList<RunStateResponse> runStateResponseList = new ArrayList<>();

    for (RecordResponse record : recordResponses) {
      RunId workflowResponse;
      UUID runId = UUID.randomUUID();

      try {
        // Build the inputs set from workflow parameter definitions and the fetched record
        Map<String, Object> workflowInputs =
            InputGenerator.buildInputs(request.getWorkflowInputDefinitions(), record);

        // Submit the workflow, get its ID and store the Run to database
        workflowResponse =
            cromwellService.submitWorkflow(runSet.methodVersion().url(), workflowInputs);
        runStateResponseList.add(
            storeRun(runId, workflowResponse.getRunId(), runSet, record.getId(), UNKNOWN, null));
      } catch (CoercionException e) {
        String errorMsg =
            String.format(
                "Input generation failed for record %s. Coercion error: %s",
                record.getId(), e.getMessage());
        log.warn(errorMsg, e);
        runStateResponseList.add(
            storeRun(runId, null, runSet, record.getId(), SYSTEM_ERROR, errorMsg + e.getMessage()));
      } catch (cromwell.client.ApiException e) {
        String errorMsg =
            String.format(
                "Cromwell submission failed for Record ID %s. ApiException: ", record.getId());
        log.warn(errorMsg, e);
        runStateResponseList.add(
            storeRun(runId, null, runSet, record.getId(), SYSTEM_ERROR, errorMsg + e.getMessage()));
      } catch (JsonProcessingException e) {
        // Should be super rare that jackson cannot convert an object to Json...
        String errorMsg =
            String.format(
                "Failed to convert inputs object to JSON for Record ID %s.", record.getId());
        log.warn(errorMsg, e);
        runStateResponseList.add(
            storeRun(runId, null, runSet, record.getId(), SYSTEM_ERROR, errorMsg + e.getMessage()));
      } catch (WorkflowAttributesNotFoundException e) {
        String errorMsg = "Attribute was not found in WDS record";
        log.warn(errorMsg, e);
        runStateResponseList.add(
            storeRun(runId, null, runSet, record.getId(), SYSTEM_ERROR, e.getMessage()));
      }
    }

    return runStateResponseList;
  }
}
