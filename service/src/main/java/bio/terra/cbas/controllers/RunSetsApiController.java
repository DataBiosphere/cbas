package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.RunId;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
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
  private static final RunState UnknownRunState = RunState.UNKNOWN;

  public RunSetsApiController(
      CromwellService cromwellService,
      WdsService wdsService,
      ObjectMapper objectMapper,
      MethodDao methodDao,
      RunDao runDao,
      RunSetDao runSetDao) {
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
    this.objectMapper = objectMapper;
    this.methodDao = methodDao;
    this.runSetDao = runSetDao;
    this.runDao = runDao;
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {

    Optional<ResponseEntity<RunSetStateResponse>> errorResponse = checkInvalidRequest(request);
    if (errorResponse.isPresent()) {
      return errorResponse.get();
    }

    // Store the method
    UUID methodId = UUID.randomUUID();

    Method method;
    try {
      method =
          new Method(
              methodId,
              request.getWorkflowUrl(),
              objectMapper.writeValueAsString(request.getWorkflowParamDefinitions()),
              request.getWdsEntities().getEntityType());
      methodDao.createMethod(method);
    } catch (JsonProcessingException e) {
      log.warn("Failed to record method to database", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Create a new run_set:
    UUID runSetId = UUID.randomUUID();
    RunSet runSet = new RunSet(runSetId, method);
    runSetDao.createRunSet(runSet);

    // Fetch the entity from WDS:
    RecordResponse recordResponse;
    try {
      String entityType = request.getWdsEntities().getEntityType();
      String entityId = request.getWdsEntities().getEntityIds().get(0);
      recordResponse = wdsService.getRecord(entityType, entityId);
    } catch (ApiException e) {
      log.warn("Entity lookup failed. ApiException", e);
      // In lieu of doing something smarter, forward on the error code from WDS:
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }

    // Build the inputs set from workflow parameter definitions and the fetched entity:
    Map<String, Object> params =
        InputGenerator.buildInputs(request.getWorkflowParamDefinitions(), recordResponse);

    // Submit the workflow and get its ID:
    RunId workflowResponse;
    try {
      workflowResponse = cromwellService.submitWorkflow(request.getWorkflowUrl(), params);
    } catch (cromwell.client.ApiException e) {
      log.warn("Cromwell submission failed. ApiException", e);
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    } catch (JsonProcessingException e) {
      // Should be super rare that jackson cannot convert an object to Json...
      log.warn("Failed to convert inputs object to JSON", e);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // Store the run:
    UUID runId = UUID.randomUUID();
    runDao.createRun(
        new Run(
            runId,
            workflowResponse.getRunId(),
            runSet,
            request.getWdsEntities().getEntityIds().get(0),
            OffsetDateTime.now(),
            UnknownRunState.toString()));

    // Return the result:
    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(runSetId.toString())
            .addRunsItem(new RunStateResponse().runId(runId.toString()).state(UnknownRunState))
            .state(RunSetState.RUNNING),
        HttpStatus.OK);
  }

  private static Optional<ResponseEntity<RunSetStateResponse>> checkInvalidRequest(
      RunSetRequest request) {
    if (request.getWdsEntities().getEntityIds().size() != 1) {
      log.warn("Bad user request: current support is exactly one entity per request");
      return Optional.of(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    return Optional.empty();
  }
}
