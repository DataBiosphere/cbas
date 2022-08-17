package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.runSets.inputs.InputGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cromwell.client.api.WorkflowsApi;
import cromwell.client.model.WorkflowIdAndStatus;
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

  private final Gson gson = new GsonBuilder().create();

  private final EntitiesApi entitiesApi;
  private final WorkflowsApi workflowsApi;

  @Autowired
  public RunSetsApiController(
      CromwellServerConfiguration cromwellConfig, WdsServerConfiguration wdsConfig) {
    this.wdsConfig = wdsConfig;
    this.cromwellConfig = cromwellConfig;

    entitiesApi = wdsConfig.entitiesApi();
    workflowsApi = cromwellConfig.workflowsApi();
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {

    for (ResponseEntity<RunSetStateResponse> errorResponse :
        checkInvalidRequest(request).stream().toList()) {
      return errorResponse;
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
    WorkflowIdAndStatus workflowResponse;
    try {
      workflowResponse = submitWorkflow(request.getWorkflowUrl(), params);
    } catch (cromwell.client.ApiException e) {
      log.warn("Cromwell submission failed. ApiException", e);
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }

    // Return the result:
    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(UUID.randomUUID().toString())
            .addRunsItem(
                new RunStateResponse()
                    .runId(workflowResponse.getId())
                    .state(cromwellToCbasRunStatus(workflowResponse.getStatus())))
            .state(RunSetState.RUNNING),
        HttpStatus.OK);
  }

  private WorkflowIdAndStatus submitWorkflow(String workflowUrl, Map<String, Object> params)
      throws cromwell.client.ApiException {

    return workflowsApi.submit(
        "v1",
        null,
        workflowUrl,
        null,
        gson.toJson(params),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static Optional<ResponseEntity<RunSetStateResponse>> checkInvalidRequest(
      RunSetRequest request) {
    if (request.getWdsEntities().getEntityIds().size() != 1) {
      log.warn("Bad user request: current support is exactly one entity per request");
      return Optional.of(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    return Optional.empty();
  }

  static RunState cromwellToCbasRunStatus(String cromwellStatus) {
    return switch (cromwellStatus) {
      case "Succeeded" -> RunState.COMPLETE;
      case "Running" -> RunState.RUNNING;
      case "Failed" -> RunState.EXECUTOR_ERROR;
      case "Submitted" -> RunState.QUEUED;
      case "Aborting" -> RunState.CANCELING;
      case "Aborted" -> RunState.CANCELED;
      default -> RunState.UNKNOWN;
    };
  }
}
