package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MetricsUtil.recordRecordsInRequest;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.common.exceptions.WorkflowAttributesNotFoundException;
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
import cromwell.client.model.RunId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.databiosphere.workspacedata.client.ApiException;
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
    if (!requestIsValid(request, this.cbasApiConfiguration)) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // Store the method
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
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Create a new run_set:
    UUID runSetId = UUID.randomUUID();
    RunSet runSet = new RunSet(runSetId, method);
    runSetDao.createRunSet(runSet);

    // Fetch the record from WDS:
    RecordResponse recordResponse;
    List<String> recordIds;
    try {
      String recordType = request.getWdsRecords().getRecordType();
      recordIds = request.getWdsRecords().getRecordIds();
      recordRecordsInRequest(recordIds.size());
      recordResponse = wdsService.getRecord(recordType, recordIds.get(0));

    } catch (ApiException e) {
      log.warn("Record lookup failed. ApiException", e);
      // In lieu of doing something smarter, forward on the error code from WDS:
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }

    // Build the inputs set from workflow parameter definitions and the fetched record:

    UUID runId = UUID.randomUUID();
    String dataTableRowId = request.getWdsRecords().getRecordIds().get(0);

    // Submit the workflow and get its ID:
    RunId workflowResponse;

    try {
      Map<String, Object> params =
          InputGenerator.buildInputs(request.getWorkflowInputDefinitions(), recordResponse);

      workflowResponse = cromwellService.submitWorkflow(request.getWorkflowUrl(), params);
    } catch (cromwell.client.ApiException e) {
      log.warn("Cromwell submission failed. ApiException", e);
      String errorMessage;
      errorMessage = e.getMessage();
      runDao.createRun(
          new Run(
              runId,
              null,
              runSet,
              dataTableRowId,
              OffsetDateTime.now(),
              SYSTEM_ERROR,
              OffsetDateTime.now(),
              OffsetDateTime.now(),
              errorMessage));
      // TODO: After WM-1448 completes, add nicer-looking return response that returns
      //  the error to the user immediately
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    } catch (JsonProcessingException e) {
      // Should be super rare that jackson cannot convert an object to Json...
      log.warn("Failed to convert inputs object to JSON", e);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (WorkflowAttributesNotFoundException e) {
      log.warn("Defined attribute not found in WDS record", e);
      String errorMessage = e.getMessage();
      runDao.createRun(
          new Run(
              runId,
              null,
              runSet,
              dataTableRowId,
              OffsetDateTime.now(),
              SYSTEM_ERROR,
              OffsetDateTime.now(),
              OffsetDateTime.now(),
              errorMessage));
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // Store the run:
    int created =
        runDao.createRun(
            new Run(
                runId,
                workflowResponse.getRunId(),
                runSet,
                dataTableRowId,
                OffsetDateTime.now(),
                UNKNOWN,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null));

    if (created != 1) {
      log.error(
          "New workflow for record {} submitted to engine as {} but CBAS database INSERT returned '{} rows created'",
          dataTableRowId,
          workflowResponse.getRunId(),
          created);
    }

    // Return the result:
    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(runSetId.toString())
            .addRunsItem(
                new RunStateResponse()
                    .runId(runId.toString())
                    .state(CbasRunStatus.toCbasApiState(UNKNOWN)))
            .state(RunSetState.RUNNING),
        HttpStatus.OK);
  }

  public static boolean requestIsValid(RunSetRequest request, CbasApiConfiguration config) {
    int recordIdsSize = request.getWdsRecords().getRecordIds().size();
    int recordIdsMax = config.getRunSetsMaximumRecordIds();
    if (recordIdsSize > recordIdsMax) {
      log.warn(
          "Bad user request: %s record IDs submitted exceeds the maximum value of %s"
              .formatted(recordIdsSize, recordIdsMax));
      return false;
    }
    return true;
  }
}
