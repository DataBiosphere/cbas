package bio.terra.cbas.dependencies.bard;

import bio.terra.bard.api.DefaultApi;
import bio.terra.bard.client.ApiClient;
import bio.terra.bard.model.EventsEvent200Response;
import bio.terra.bard.model.EventsEventLogRequest;
import bio.terra.cbas.config.BardServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.common.iam.BearerToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BardService implements HealthCheck {

  Logger log = LoggerFactory.getLogger(BardService.class);

  private final BardClient bardClient;
  private final BardServerConfiguration bardServerConfiguration;
  private final String appId = "cbas";

  public BardService(BardClient bardClient, BardServerConfiguration bardServerConfiguration) {
    this.bardClient = bardClient;
    this.bardServerConfiguration = bardServerConfiguration;
  }

  private DefaultApi getDefaultAuthApi(BearerToken userToken) {
    ApiClient client = bardClient.bardAuthClient(userToken);
    return bardClient.defaultApi(client);
  }

  public void logRunSetEvent(
      RunSetRequest request,
      MethodVersion methodVersion,
      List<String> workflowIds,
      BearerToken userToken) {
    String eventName = "workflow-submission";
    HashMap<String, String> properties = new HashMap<>();
    properties.put("runSetName", request.getRunSetName());
    properties.put("methodName", methodVersion.method().name());
    properties.put("methodSource", methodVersion.method().methodSource());
    properties.put("methodVersionName", methodVersion.name());
    properties.put("methodVersionUrl", methodVersion.url());
    properties.put("recordCount", String.valueOf(request.getWdsRecords().getRecordIds().size()));
    properties.put("workflowIds", workflowIds.toString());

    Optional<GithubMethodDetails> maybeGitHubMethodDetails =
        methodVersion.method().githubMethodDetails();
    if (maybeGitHubMethodDetails.isPresent()) {
      GithubMethodDetails githubMethodDetails = maybeGitHubMethodDetails.get();
      properties.put("githubOrganization", githubMethodDetails.organization());
      properties.put("githubRepository", githubMethodDetails.repository());
      properties.put("githubIsPrivate", githubMethodDetails.isPrivate().toString());
    }

    logEvent(eventName, properties, userToken);
  }

  public void logEvent(String eventName, Map<String, String> properties, BearerToken userToken) {
    if (bardServerConfiguration.enabled()) {
      EventsEventLogRequest request = new EventsEventLogRequest().properties(properties);
      try {
        getDefaultAuthApi(userToken).eventsEventLog(eventName, appId, request);
      } catch (Exception e) {
        log.warn("Error logging event {} ", eventName, e);
      }
    }
  }

  @Override
  public Result checkHealth() {
    try {
      ApiClient client = bardClient.apiClient();
      DefaultApi defaultApi = bardClient.defaultApi(client);
      EventsEvent200Response status = defaultApi.systemStatus();
      return new Result(true, "Ok");
    } catch (Exception e) {
      return new Result(false, e.getMessage());
    }
  }
}
