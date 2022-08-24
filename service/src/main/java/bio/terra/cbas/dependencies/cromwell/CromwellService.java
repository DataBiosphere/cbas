package bio.terra.cbas.dependencies.cromwell;

import bio.terra.cbas.model.RunState;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import cromwell.client.model.WorkflowIdAndStatus;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CromwellService {

  private final CromwellClient cromwellClient;

  public CromwellService(CromwellClient cromwellClient) {
    this.cromwellClient = cromwellClient;
  }

  public WorkflowIdAndStatus submitWorkflow(String workflowUrl, Map<String, Object> params)
      throws cromwell.client.ApiException, JsonProcessingException {

    return cromwellClient
        .workflowsApi()
        .submit(
            "v1",
            null,
            workflowUrl,
            null,
            InputGenerator.inputsToJson(params),
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

  public static RunState cromwellToCbasRunStatus(String cromwellStatus) {
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
