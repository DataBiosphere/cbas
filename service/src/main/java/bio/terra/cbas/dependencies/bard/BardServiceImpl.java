package bio.terra.cbas.dependencies.bard;

import bio.terra.bard.api.DefaultApi;
import bio.terra.bard.client.ApiClient;
import bio.terra.bard.model.EventsEventLogRequest;
import bio.terra.cbas.config.BardServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.iam.BearerToken;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class BardServiceImpl implements BardService, HealthCheck {

  Logger log = LoggerFactory.getLogger(BardServiceImpl.class);
  private static final String appId = "cbas";
  private final BardClient bardClient;
  private final BardServerConfiguration bardServerConfiguration;

  public BardServiceImpl(BardClient bardClient, BardServerConfiguration bardServerConfiguration) {
    this.bardClient = bardClient;
    this.bardServerConfiguration = bardServerConfiguration;
  }

  private DefaultApi getDefaultAuthApi(BearerToken userToken) {
    ApiClient client = bardClient.bardAuthClient(userToken);
    return bardClient.defaultApi(client);
  }

  @Async("bardAsyncExecutor")
  @Override
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
      defaultApi.systemStatus();
      return new Result(true, "Ok");
    } catch (Exception e) {
      return new Result(false, e.getMessage());
    }
  }
}
