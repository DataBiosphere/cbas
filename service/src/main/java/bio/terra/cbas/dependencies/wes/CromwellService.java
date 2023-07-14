package bio.terra.cbas.dependencies.wes;

import static bio.terra.cbas.api.RunsApi.log;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  public RunId submitWorkflow(
      String workflowUrl, Map<String, Object> params, Boolean isCallCachingEnabled)
      throws ApiException, JsonProcessingException {

    return cromwellClient
        .wesAPI()
        .runWorkflow(
            InputGenerator.inputsToJson(params),
            null,
            null,
            null,
            this.buildWorkflowOptionsJson(
                cromwellClient.getFinalWorkflowLogDirOption(), isCallCachingEnabled),
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

  /**
   * Cromwell accepts an object "Workflow Options" to specify additional configuration for a
   * workflow. Here, we build that object with the parameters we care about. final_workflow_log_dir
   * specifies the path where outputs will be written. write_to_cache and read_from_cache are both
   * related to call caching. When a user enables call caching, these should both be set to true.
   * Otherwise, they should both be set to false.
   * https://cromwell.readthedocs.io/en/stable/wf_options/Overview/ for more info.
   *
   * @param isCallCachingEnabled Whether the user wishes to run this workflow with call caching.
   * @return A string formatted as a JSON object that can be used as cromwell's Workflow Options.
   * @throws JsonProcessingException, IllegalArgumentException
   */
  public static String buildWorkflowOptionsJson(
      Optional<String> finalWorkflowLogDir, Boolean isCallCachingEnabled)
      throws JsonProcessingException {
    if (isCallCachingEnabled == null) {
      log.error(
          "Sending null call caching parameters to Cromwell. call_caching_enabled should be set to a boolean value. ");
    }
    Map<String, Object> workflowOptions = new HashMap<>();
    // This supplies a JSON snippet to WES to use as workflowOptions for a cromwell submission
    if (finalWorkflowLogDir.isPresent()) {
      workflowOptions.put("final_workflow_log_dir", finalWorkflowLogDir.get());
    }
    workflowOptions.put("write_to_cache", isCallCachingEnabled);
    workflowOptions.put("read_from_cache", isCallCachingEnabled);
    return InputGenerator.inputsToJson(workflowOptions);
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
