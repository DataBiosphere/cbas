package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import cromwell.client.ApiClient;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import cromwell.client.api.WorkflowsApi;
import cromwell.client.model.RunId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.databiosphere.workspacedata.api.EntitiesApi;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.EntityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunSetsApiController implements RunSetsApi {
  private final WdsServerConfiguration wdsConfig;
  private final CromwellServerConfiguration cromwellConfig;

  private final EntitiesApi entitiesApi;
  private final WorkflowsApi workflowsApi;

  private final Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesApi;

  @Autowired
  public RunSetsApiController(
      CromwellServerConfiguration cromwellConfig, WdsServerConfiguration wdsConfig) {
    this.wdsConfig = wdsConfig;
    this.cromwellConfig = cromwellConfig;

    entitiesApi = wdsConfig.entitiesApi();
    workflowsApi = cromwellConfig.workflowsApi();

    ApiClient client = new ApiClient();
    client.setBasePath(this.cromwellConfig.baseUri());
    wesApi = new Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi(client);
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {

    Optional<ResponseEntity<RunSetStateResponse>> errorResponse = checkInvalidRequest(request);
    if (errorResponse.isPresent()) {
      return errorResponse.get();
    }

    // Fetch the entity from WDS:
    EntityResponse entityResponse;
    try {
      String entityType = request.getWdsEntities().getEntityType();
      String entityId = request.getWdsEntities().getEntityIds().get(0);
      entityResponse =
          entitiesApi.getEntity(wdsConfig.instanceId(), wdsConfig.apiV(), entityType, entityId);
    } catch (ApiException e) {
      log.warn("Entity lookup failed. ApiException", e);
      // In lieu of doing something smarter, forward on the error code from WDS:
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }

    // Build the inputs set from workflow parameter definitions and the fetched entity:
    Map<String, Object> params =
        InputGenerator.buildInputs(request.getWorkflowParamDefinitions(), entityResponse);

    // Submit the workflow and get its ID:
    RunId workflowResponse;
    try {
      workflowResponse = submitWorkflow(request.getWorkflowUrl(), params);
    } catch (cromwell.client.ApiException e) {
      log.warn("Cromwell submission failed. ApiException", e);
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    } catch (JsonProcessingException e) {
      // Should be super rare that jackson cannot convert an object to Json...
      log.warn("Failed to convert inputs object to JSON", e);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

//    RunStateResponse state = new RunStateResponse();

    // Return the result:
    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(UUID.randomUUID().toString())
            .addRunsItem(
                new RunStateResponse().runId(workflowResponse.getRunId()).state(RunState.RUNNING))
            .state(RunSetState.RUNNING),
        HttpStatus.OK);
  }

  private RunId submitWorkflow(String workflowUrl, Map<String, Object> params)
      throws cromwell.client.ApiException, JsonProcessingException {

    return wesApi.runWorkflow(
        InputGenerator.inputsToJson(params), null, null, null, null, workflowUrl, null);
  }

  private static Optional<ResponseEntity<RunSetStateResponse>> checkInvalidRequest(
      RunSetRequest request) {
    if (request.getWdsEntities().getEntityIds().size() != 1) {
      log.warn("Bad user request: current support is exactly one entity per request");
      return Optional.of(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    return Optional.empty();
  }

  //  static RunState cromwellToCbasRunStatus(String cromwellStatus) {
  //    return switch (cromwellStatus) {
  //      case "Succeeded" -> RunState.COMPLETE;
  //      case "Running" -> RunState.RUNNING;
  //      case "Failed" -> RunState.EXECUTOR_ERROR;
  //      case "Submitted" -> RunState.QUEUED;
  //      case "Aborting" -> RunState.CANCELING;
  //      case "Aborted" -> RunState.CANCELED;
  //      default -> RunState.UNKNOWN;
  //    };
  //  }
}
