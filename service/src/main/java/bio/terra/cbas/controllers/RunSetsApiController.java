package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionEntityLookup;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.model.WorkflowParamDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cromwell.client.api.WorkflowsApi;
import cromwell.client.model.WorkflowIdAndStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.databiosphere.workspacedata.api.EntitiesApi;
import org.databiosphere.workspacedata.client.ApiClient;
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
  private final String workspaceId = "15f36863-30a5-4cab-91f7-52be439f1175";
  private final String wdsApiV = "v0.2";

  private final Gson gson = new GsonBuilder().create();

  @Autowired
  public RunSetsApiController(
      CromwellServerConfiguration cromwellConfig, WdsServerConfiguration wdsConfig) {
    this.wdsConfig = wdsConfig;
    this.cromwellConfig = cromwellConfig;
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {

    if (request.getWdsEntities().getEntityIds().size() != 1) {
      log.warn("Bad user request: current support is exactly one entity per request");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    String entityType = request.getWdsEntities().getEntityType();
    String entityId = request.getWdsEntities().getEntityIds().get(0);

    HashMap<String, Object> entityAttributes;

    ApiClient wdsApiClient = new ApiClient();
    wdsApiClient.setBasePath(wdsConfig.baseUri());

    EntitiesApi entitiesApi = new EntitiesApi(wdsApiClient);

    EntityResponse entityResponse;

    try {
      entityResponse = entitiesApi.getEntity(workspaceId, wdsApiV, entityType, entityId);
    } catch (ApiException e) {
      log.warn("Entity lookup failed. ApiException", e);
      // In lieu of doing something smarter, forward on the error code from WDS:
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }

    Map<String, Object> params = new HashMap<>();
    for (WorkflowParamDefinition param : request.getWorkflowParamDefinitions()) {
      String parameterName = param.getParameterName();
      Object parameterValue;
      if (param.getSource().getType() == ParameterDefinition.TypeEnum.LITERAL) {
        parameterValue = ((ParameterDefinitionLiteralValue) param.getSource()).getParameterValue();
      } else {
        String attributeName =
            ((ParameterDefinitionEntityLookup) param.getSource()).getEntityAttribute();
        parameterValue = entityResponse.getAttributes().get(attributeName);
      }
      params.put(parameterName, parameterValue);
    }

    String paramsJson = gson.toJson(params);
    log.info("Constructed inputs from WDS entity: " + paramsJson);

    cromwell.client.ApiClient client = new cromwell.client.ApiClient();
    client.setBasePath(this.cromwellConfig.baseUri());
    WorkflowsApi workflowsApi = new WorkflowsApi(client);
    String runId = UUID.randomUUID().toString();

    WorkflowIdAndStatus workflowResponse;

    try {
      workflowResponse =
          workflowsApi.submit(
              "v1",
              null,
              request.getWorkflowUrl(),
              null,
              paramsJson,
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
    } catch (cromwell.client.ApiException e) {
      log.warn("Cromwell submission failed. ApiException", e);
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }

    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(UUID.randomUUID().toString())
            .addRunsItem(
                new RunStateResponse().runId(UUID.randomUUID().toString()).state(RunState.RUNNING))
            .state(RunSetState.RUNNING),
        HttpStatus.OK);
  }
}
