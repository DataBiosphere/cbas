package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MetricsUtil.recordRecordsInRequest;
import static bio.terra.cbas.model.RunSetState.ERROR;
import static bio.terra.cbas.model.RunSetState.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import cromwell.client.model.RunId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final MethodDao methodDao;
  private final RunSetDao runSetDao;
  private final RunDao runDao;
  private final ObjectMapper objectMapper;
  private final CbasApiConfiguration cbasApiConfiguration;

  public RunSetsApiController(
      CromwellService cromwellService,
      WdsService wdsService,
      ObjectMapper objectMapper,
      MethodDao methodDao,
      RunDao runDao,
      RunSetDao runSetDao,
      CbasApiConfiguration cbasApiConfiguration) {
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
    this.objectMapper = objectMapper;
    this.methodDao = methodDao;
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.cbasApiConfiguration = cbasApiConfiguration;
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {
    Gson gson = new Gson();

    // request validation
    Optional<ResponseEntity<RunSetStateResponse>> validateRequestResponse =
        validateRequest(request, this.cbasApiConfiguration.getRunSetsMaximumRecordIds());
    if (validateRequestResponse.isPresent()) {
      return validateRequestResponse.get();
    }

    // Fetch WDS Records and keep track of errors while retrieving records
    String recordType = request.getWdsRecords().getRecordType();
    List<String> recordIds = request.getWdsRecords().getRecordIds();
    recordRecordsInRequest(recordIds.size());

    ArrayList<RecordResponse> recordResponses = new ArrayList<>();
    HashMap<String, String> recordIdsWithError = new HashMap<>();
    for (String recordId : recordIds) {
      try {
        recordResponses.add(wdsService.getRecord(recordType, recordId));
      } catch (ApiException e) {
        log.warn("Record lookup for Record ID {} failed.", recordId, e);
        ErrorResponse error = gson.fromJson(e.getResponseBody(), ErrorResponse.class);
        String errorMsg;
        if (error == null) {
          errorMsg = e.getMessage();
        } else {
          errorMsg = error.getMessage();
        }
        recordIdsWithError.put(recordId, errorMsg);
      }
    }

    if (recordIdsWithError.size() > 0) {
      String errorMsg = "Error while fetching WDS Records for Record ID(s): " + recordIdsWithError;
      log.warn(errorMsg);
      return new ResponseEntity<>(
          new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST);
    }

    // Store new method
    UUID methodId = UUID.randomUUID();
    Method method;
    try {
      method =
          new Method(
              methodId,
              request.getWorkflowUrl(),
              objectMapper.writeValueAsString(request.getWorkflowInputDefinitions()),
              objectMapper.writeValueAsString(request.getWorkflowOutputDefinitions()),
              request.getWdsRecords().getRecordType());
      methodDao.createMethod(method);
    } catch (JsonProcessingException e) {
      log.warn("Failed to record method to database", e);
      return new ResponseEntity<>(
          new RunSetStateResponse()
              .errors("Failed to record method to database. Error(s): " + e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Create a new run_set
    UUID runSetId = UUID.randomUUID();
    RunSet runSet = new RunSet(runSetId, method);
    runSetDao.createRunSet(runSet);

    // For each Record ID, build workflow inputs and submit the workflow to Cromwell
    List<RunStateResponse> runStateResponseList =
        buildInputsAndSubmitRun(request, runSet, recordResponses);

    // Figure out how many runs are in Failed state. If all Runs are in an Error state then mark the
    // Run Set as Failed
    RunSetState runSetState;
    List<RunStateResponse> runsInErrorState =
        runStateResponseList.stream()
            .filter(run -> CbasRunStatus.fromValue(run.getState()).equals(SYSTEM_ERROR))
            .toList();

    if (runsInErrorState.size() == recordIds.size()) {
      runSetState = ERROR;
    } else runSetState = RUNNING;

    // Return the result
    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(runSetId.toString())
            .runs(runStateResponseList)
            .state(runSetState),
        HttpStatus.OK);
  }

  public static Optional<ResponseEntity<RunSetStateResponse>> validateRequest(
      RunSetRequest request, int maxRecordIds) {
    String errorMsg = "";

    // check number of Record IDs in request is within allowed limit
    int recordIdsSize = request.getWdsRecords().getRecordIds().size();
    if (recordIdsSize > maxRecordIds) {
      errorMsg =
          "%s Record IDs submitted exceeds the maximum value of %s. "
              .formatted(recordIdsSize, maxRecordIds);
    }

    // check that there are no duplicated Record IDs present in the request
    List<String> recordIds = request.getWdsRecords().getRecordIds();
    List<String> duplicateRecordIds =
        recordIds.stream().filter(e -> Collections.frequency(recordIds, e) > 1).distinct().toList();
    if (duplicateRecordIds.size() > 0) {
      errorMsg +=
          String.format("Duplicate Record ID(s) %s present in request.", duplicateRecordIds);
    }

    if (!errorMsg.isEmpty()) {
      String finalErrorMsg = "Bad user request. Error(s): " + errorMsg;
      log.warn(finalErrorMsg);
      return Optional.of(
          new ResponseEntity<>(
              new RunSetStateResponse().errors(finalErrorMsg), HttpStatus.BAD_REQUEST));
    }

    return Optional.empty();
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
                OffsetDateTime.now(),
                runState,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                errors));

    if (created != 1) {
      additionalErrorMsg =
          String.format(
              "CBAS failed to create new row for Record ID %s in %s state in database. INSERT returned '%s rows created'",
              recordId, runState, created);
      log.error(additionalErrorMsg);
    }

    return new RunStateResponse()
        .runId(runId.toString())
        .state(CbasRunStatus.toCbasApiState(runState))
        .errors(errors + additionalErrorMsg);
  }

  private List<RunStateResponse> buildInputsAndSubmitRun(
      RunSetRequest request, RunSet runSet, ArrayList<RecordResponse> recordResponses) {
    ArrayList<RunStateResponse> runStateResponseList = new ArrayList<>();

    for (RecordResponse record : recordResponses) {
      // Build the inputs set from workflow parameter definitions and the fetched record
      Map<String, Object> workflowInputs =
          InputGenerator.buildInputs(request.getWorkflowInputDefinitions(), record);

      // Submit the workflow, get its ID and store the Run to database
      RunId workflowResponse;
      UUID runId = UUID.randomUUID();
      try {
        workflowResponse = cromwellService.submitWorkflow(request.getWorkflowUrl(), workflowInputs);
        runStateResponseList.add(
            storeRun(runId, workflowResponse.getRunId(), runSet, record.getId(), UNKNOWN, null));
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
      }
    }

    return runStateResponseList;
  }
}
