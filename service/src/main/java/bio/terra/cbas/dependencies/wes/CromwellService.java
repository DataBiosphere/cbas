package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cromwell.client.ApiException;
import cromwell.client.model.FailureMessage;
import cromwell.client.model.RunId;
import cromwell.client.model.WorkflowDescription;
import cromwell.client.model.WorkflowMetadataResponse;
import cromwell.client.model.WorkflowQueryResult;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CromwellService implements HealthCheck {

  private static final Integer MAX_ALLOWED_CHARACTERS = 100;
  private final CromwellClient cromwellClient;
  private final CromwellServerConfiguration cromwellConfig;

  private static final String API_VERSION = "v1";

  public CromwellService(
      CromwellClient cromwellClient, CromwellServerConfiguration cromwellConfig) {
    this.cromwellClient = cromwellClient;
    this.cromwellConfig = cromwellConfig;
  }

  public RunId submitWorkflow(String workflowUrl, Map<String, Object> params, Boolean isCallCachingEnabled)
      throws ApiException, JsonProcessingException {

    //build the Workflow Options JSON
    Map<String, Object> workflowOptions = new HashMap<>();
     // This supplies a JSON snippet to WES to use as workflowOptions for a cromwell submission
    workflowOptions.put("final_workflow_log_dir", this.cromwellClient.getFinalWorkflowLogDirOption().orElse(null));
    workflowOptions.put("write_to_cache", isCallCachingEnabled);
    workflowOptions.put("read_from_cache", isCallCachingEnabled);

    return cromwellClient
        .wesAPI()
        .runWorkflow(
            InputGenerator.inputsToJson(params),
            null,
            null,
            null,
            InputGenerator.inputsToJson(workflowOptions),
            workflowUrl,
            null);
  }

  public WorkflowQueryResult runSummary(String runId) throws ApiException {
    var queryResults =
        cromwellConfig
            .workflowsApi()
            .queryGet(
                API_VERSION,
                null,
                null,
                null,
                null,
                null,
                List.of(runId),
                null,
                null,
                null,
                null,
                null,
                null)
            .getResults();

    if (queryResults.isEmpty()) {
      return null;
    } else {
      return queryResults.get(0);
    }
  }

  public Object getOutputs(String id) throws ApiException {
    return cromwellClient.wesAPI().getRunLog(id).getOutputs();
  }

  public WorkflowDescription describeWorkflow(String workflowUrl) throws ApiException {
    return cromwellConfig.womtoolApi().describe(API_VERSION, null, workflowUrl, null, null, null);
  }

  public String getRunErrors(Run run) throws ApiException {

    WorkflowMetadataResponse meta =
        cromwellConfig
            .workflowsApi()
            .metadata(
                API_VERSION, run.engineId(), Collections.singletonList("failure"), null, null);

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

  public void cancelRun(Run run) throws ApiException {
    cromwellClient.wesAPI().cancelRun(run.engineId());
  }

  @Override
  public Result checkHealth() {
    try {
      // No response, the successful return code is the important thing:
      cromwellClient.engineApi().engineStatus("v1");
      return new Result(true, "Cromwell was reachable");
    } catch (ApiException e) {
      return new Result(false, e.getMessage());
    }
  }
}
