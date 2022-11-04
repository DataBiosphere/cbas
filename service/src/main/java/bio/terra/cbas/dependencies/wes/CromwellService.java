package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import cromwell.client.ApiException;
import cromwell.client.model.FailureMessage;
import cromwell.client.model.RunId;
import cromwell.client.model.RunStatus;
import cromwell.client.model.WorkflowMetadataResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CromwellService {

  private static final Integer MAX_ALLOWED_CHARACTERS = 100;
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

    WorkflowMetadataResponse meta =
        cromwellConfig
            .workflowsApi()
            .metadata("v1", run.engineId(), Collections.singletonList("failure"), null, null);

    return getErrorMessage(meta.getFailures());
  }

  public static String getErrorMessage(List<FailureMessage> failureMessages) {

    if (failureMessages == null || failureMessages.isEmpty()) {
      return "";
    }

    String failureMessage =
        failureMessages.get(0).getMessage() == null ? "" : failureMessages.get(0).getMessage();
    String causedByMessage = getErrorMessage(failureMessages.get(0).getCausedBy());

    StringBuilder sb = new StringBuilder();

    sb.append(failureMessage);

    if (!causedByMessage.isEmpty()) {
      sb.append(" (");
      sb.append(causedByMessage);
      sb.append(")");
    }

    String result = sb.toString();

    if (result.length() > MAX_ALLOWED_CHARACTERS) {
      return result.substring(0, MAX_ALLOWED_CHARACTERS - 3) + "...";
    } else {
      return result;
    }
  }
}
