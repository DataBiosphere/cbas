package bio.terra.cbas.dependencies.wes;

import static bio.terra.cbas.api.RunsApi.log;

import bio.terra.cbas.config.CbasNetworkConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.inputs.InputGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import cromwell.client.ApiClient;
import cromwell.client.ApiException;
import cromwell.client.model.FailureMessage;
import cromwell.client.model.WorkflowDescription;
import cromwell.client.model.WorkflowIdAndStatus;
import cromwell.client.model.WorkflowMetadataResponse;
import cromwell.client.model.WorkflowQueryResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CromwellService implements HealthCheck {

  private static final Integer MAX_ALLOWED_CHARACTERS = 100;
  private final CromwellClient cromwellClient;
  private static final String API_VERSION = "v1";
  private final ApiClient cromwellWriteClient;

  private final CbasNetworkConfiguration cbasNetworkConfiguration;

  public CromwellService(
      CromwellClient cromwellClient,
      ApiClient cromwellWriteClient,
      CbasNetworkConfiguration cbasNetworkConfiguration) {
    this.cromwellClient = cromwellClient;
    this.cromwellWriteClient = cromwellWriteClient;
    this.cbasNetworkConfiguration = cbasNetworkConfiguration;
  }

  public List<WorkflowIdAndStatus> submitWorkflowBatch(
      String workflowUrl, Map<UUID, String> requestedIdToWorkflowInput, String workflowOptionsJson)
      throws ApiException {

    // Ensure the order of inputs and ids passed to the endpoint are identical
    List<UUID> requestedIdOrder = requestedIdToWorkflowInput.keySet().stream().toList();

    return cromwellClient
        .workflowsApi(cromwellWriteClient)
        .submitBatch(
            API_VERSION,
            requestedIdOrder.stream()
                .map(requestedIdToWorkflowInput::get)
                .collect(Collectors.joining(",", "[", "]")),
            null,
            workflowUrl,
            null,
            workflowOptionsJson,
            null,
            null,
            null,
            null,
            requestedIdOrder.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(",", "[", "]")));
  }

  public WorkflowQueryResult runSummary(String runId) throws ApiException {
    ApiClient client = cromwellClient.getReadApiClient();
    var queryResults =
        cromwellClient
            .workflowsApi(client)
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
    ApiClient client = cromwellClient.getReadApiClient();

    return cromwellClient.wesAPI(client).getRunLog(id).getOutputs();
  }

  public WorkflowDescription describeWorkflow(String workflowUrl) throws ApiException {
    ApiClient client = cromwellClient.getReadApiClient();
    return cromwellClient
        .womtoolApi(client)
        .describe(API_VERSION, null, workflowUrl, null, null, null);
  }

  public String getRunErrors(Run run) throws ApiException {

    ApiClient client = cromwellClient.getReadApiClient();

    WorkflowMetadataResponse meta =
        cromwellClient
            .workflowsApi(client)
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
   * related to call caching. Users expect that we will always write_to_cache, but only
   * read_from_cache when call caching is enabled. This is how it works in GCP, so we are mirroring
   * the behavior here. write_from_cache should always be true, and read_from_cache should be false
   * if the user doesn't wish to call cache. <a
   * href="https://cromwell.readthedocs.io/en/stable/wf_options/Overview/">...</a> for more info.
   *
   * @param isCallCachingEnabled Whether the user wishes to run this workflow with call caching.
   * @return A string formatted as a JSON object that can be used as cromwell's Workflow Options.
   * @throws JsonProcessingException
   */
  public String buildWorkflowOptionsJson(Boolean isCallCachingEnabled) {
    // Map we will convert to JSON
    Map<String, Object> workflowOptions =
        new HashMap<>(
            Map.ofEntries(
                Map.entry("write_to_cache", true),
                Map.entry("read_from_cache", isCallCachingEnabled)));

    // Request a callback, if we know our own URL:
    cbasNetworkConfiguration
        .getCallbackUri()
        .ifPresent(s -> workflowOptions.put("workflow_callback_uri", s));

    // Path for cromwell to write workflow logs to.
    Optional<String> finalWorkflowLogDir = cromwellClient.getFinalWorkflowLogDirOption();
    finalWorkflowLogDir.ifPresent(s -> workflowOptions.put("final_workflow_log_dir", s));

    try {
      return InputGenerator.inputsToJson(workflowOptions);
    } catch (JsonProcessingException e) {
      String errorMsg =
          String.format(
              "Failed to generate Workflow Options JSON. JsonProcessingException: %s",
              e.getMessage());
      log.warn(errorMsg, e);
      return "{}";
    }
  }

  public void cancelRun(Run run) throws ApiException {
    cromwellClient.wesAPI(cromwellWriteClient).cancelRun(run.engineId());
  }

  @Override
  public Result checkHealth() {
    try {
      ApiClient client = cromwellClient.getReadApiClient();
      // No response, the successful return code is the important thing:
      cromwellClient.engineApi(client).engineStatus("v1");
      return new Result(true, "Cromwell was reachable");
    } catch (ApiException e) {
      return new Result(false, e.getMessage());
    }
  }
}
