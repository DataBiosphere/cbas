package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.ApiClient;
import cromwell.client.ApiException;
import cromwell.client.api.WorkflowsApi;
import cromwell.client.model.RunId;
import cromwell.client.model.RunStatus;
import cromwell.client.model.WorkflowMetadataResponse;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public class CromwellService {

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final CromwellClient cromwellClient;
  private final CromwellServerConfiguration cromwellConfig;

  public CromwellService(
      CromwellClient cromwellClient, CromwellServerConfiguration cromwellConfig) {
    this.cromwellClient = cromwellClient;
    this.cromwellConfig = cromwellConfig;
  }

  public RunId submitWorkflow(String workflowUrl, Map<String, Object> params)
      throws ApiException, JsonProcessingException {

    return cromwellClient
        .wesAPI()
        .runWorkflow(
            InputGenerator.inputsToJson(params), null, null, null, null, workflowUrl, null);
  }

  public RunStatus runStatus(String runId) throws ApiException {
    return cromwellClient.wesAPI().getRunStatus(runId);
  }

  public Object getOutputs(String id) throws ApiException {
    return cromwellClient.wesAPI().getRunLog(id).getOutputs();
  }

  public String getRunErrors(Run run) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(this.cromwellConfig.baseUri());
    WorkflowsApi cromwellApi = new WorkflowsApi(client);

    WorkflowMetadataResponse meta =
        cromwellApi.metadata(
            "v1", run.engineId(), Collections.singletonList("failure"), null, null);

    return meta.getFailures().get(0).getCausedBy().get(0).getMessage();
  }
}
