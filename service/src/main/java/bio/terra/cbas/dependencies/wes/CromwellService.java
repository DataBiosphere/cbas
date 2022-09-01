package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import cromwell.client.model.RunId;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CromwellService {

  private final CromwellClient cromwellClient;

  public CromwellService(CromwellClient cromwellClient) {
    this.cromwellClient = cromwellClient;
  }

  public RunId submitWorkflow(String workflowUrl, Map<String, Object> params)
      throws cromwell.client.ApiException, JsonProcessingException {

    return cromwellClient
        .wesAPI()
        .runWorkflow(
            InputGenerator.inputsToJson(params), null, null, null, null, workflowUrl, null);
  }
}
