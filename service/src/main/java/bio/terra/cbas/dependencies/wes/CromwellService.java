package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheckable;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import cromwell.client.ApiException;
import cromwell.client.model.FailureMessage;
import cromwell.client.model.RunId;
import cromwell.client.model.WorkflowDescription;
import cromwell.client.model.WorkflowMetadataResponse;
import cromwell.client.model.WorkflowQueryResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CromwellService implements HealthCheckable {

  private static final Integer MAX_ALLOWED_CHARACTERS = 100;
  private final CromwellClient cromwellClient;
  private final CromwellServerConfiguration cromwellConfig;

  private static final String apiVersion = "v1";

  public CromwellService(
      CromwellClient cromwellClient, CromwellServerConfiguration cromwellConfig) {
    this.cromwellClient = cromwellClient;
    this.cromwellConfig = cromwellConfig;
  }

  public RunId submitWorkflow(String workflowUrl, Map<String, Object> params)
      throws ApiException, JsonProcessingException {

    // This supplies a JSON snippet to WES to use as workflowOptions for a cromwell submission
    String workflowOptions =
        this.cromwellClient
            .getFinalWorkflowLogDirOption()
            .map(dir -> String.format("{\"final_workflow_log_dir\": \"%s\"}", dir))
            .orElse(null);

    return cromwellClient
        .wesAPI()
        .runWorkflow(
            InputGenerator.inputsToJson(params),
            null,
            null,
            null,
            workflowOptions,
            workflowUrl,
            null);
  }

  public WorkflowQueryResult runSummary(String runId) throws ApiException {
    var queryResults =
        cromwellConfig
            .workflowsApi()
            .queryGet(
                apiVersion,
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
    return cromwellConfig.womtoolApi().describe(apiVersion, null, workflowUrl, null, null, null);
  }

  public String getRunErrors(Run run) throws ApiException {

    WorkflowMetadataResponse meta =
        cromwellConfig
            .workflowsApi()
            .metadata(apiVersion, run.engineId(), Collections.singletonList("failure"), null, null);

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
  public HealthCheckable.HealthCheckResult checkHealth() {
    try {
      // No response, the successful return code is the important thing:
      cromwellClient.engineApi().engineStatus("v1");
      return new HealthCheckResult(true, "Cromwell was reachable");
    } catch (ApiException e) {
      return new HealthCheckable.HealthCheckResult(false, e.getMessage());
    }
  }
}
