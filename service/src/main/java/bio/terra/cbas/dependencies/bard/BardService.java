package bio.terra.cbas.dependencies.bard;

import bio.terra.bard.api.DefaultApi;
import bio.terra.bard.client.ApiClient;
import bio.terra.bard.model.EventsEvent200Response;
import bio.terra.bard.model.EventsEventLogRequest;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.common.iam.BearerToken;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class BardService implements HealthCheck {

  Logger log = LoggerFactory.getLogger(BardService.class);

  private final BardClient bardClient;
  private final String appId = "cbas";

  public BardService(BardClient bardClient) {
    this.bardClient = bardClient;
  }

  private DefaultApi getDefaultApi(BearerToken userToken) {
    ApiClient client = bardClient.bardAuthClient(userToken);
    return bardClient.defaultApi(client);
  }

  public void logRunSetEvent(RunSetRequest request, BearerToken userToken) {
    String eventName = "workflow-submission";
    HashMap<String, String> properties = new HashMap<>();
    properties.put("runSetName", request.getRunSetName());
    properties.put("methodVersionId", request.getMethodVersionId().toString());
    properties.put("wdsRecords", String.valueOf(request.getWdsRecords().getRecordIds().size()));
    logEvent(eventName, properties, userToken);
  }

  public void logEvent(String eventName, Map properties, BearerToken userToken) {
    EventsEventLogRequest request = new EventsEventLogRequest().properties(properties);
    try {
      getDefaultApi(userToken).eventsEventLog(eventName, appId, request);
    } catch (RestClientException e) {
      log.warn("Error logging event {} ", eventName, e);
    }
  }

  @Override
  public Result checkHealth() {
    try {
      ApiClient client = bardClient.apiClient();
      DefaultApi defaultApi = bardClient.defaultApi(client);
      EventsEvent200Response status = defaultApi.systemStatus();
      return new Result(true, status.toString());
    } catch (RestClientException e) {
      return new Result(false, e.getMessage());
    }
  }
}
