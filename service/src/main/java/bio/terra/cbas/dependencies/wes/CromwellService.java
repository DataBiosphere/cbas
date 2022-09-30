package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import cromwell.client.ApiException;
import cromwell.client.model.RunId;
import cromwell.client.model.RunStatus;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CromwellService {

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
}
