package bio.terra.cbas.dependencies.leonardo;

import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.api.AppsApi;
import org.broadinstitute.dsde.workbench.client.leonardo.api.ServiceInfoApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.SystemStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class LeonardoService implements HealthCheck {

  private final LeonardoClient leonardoClient;
  private final RetryTemplate listenerResetRetryTemplate;

  private final WdsServerConfiguration wdsServerConfiguration;

  public LeonardoService(
      LeonardoClient leonardoClient,
      WdsServerConfiguration wdsServerConfiguration,
      RetryTemplate listenerResetRetryTemplate) {
    this.leonardoClient = leonardoClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.listenerResetRetryTemplate = listenerResetRetryTemplate;
  }

  AppsApi getAppsApi(String userToken) {
    return new AppsApi(leonardoClient.getApiClient(userToken));
  }

  private ServiceInfoApi getServiceInfoApi() {
    return new ServiceInfoApi(leonardoClient.getUnauthorizedApiClient());
  }

  public List<ListAppResponse> getApps(String userToken, boolean creatorOnly)
      throws LeonardoServiceException {
    String creatorRoleSpecifier = creatorOnly ? "creator" : null;
    return executionWithRetryTemplate(
        listenerResetRetryTemplate,
        () ->
            getAppsApi(userToken)
                .listAppsV2(
                    wdsServerConfiguration.instanceId(), null, null, null, creatorRoleSpecifier));
  }

  @Override
  public Result checkHealth() {
    try {
      SystemStatus result = getServiceInfoApi().getSystemStatus();
      return new Result(result.getOk(), result.toString());
    } catch (ApiException e) {
      return new Result(false, e.getMessage());
    }
  }

  interface LeonardoAction<T> {
    T execute() throws org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
  }

  static <T> T executionWithRetryTemplate(
      RetryTemplate retryTemplate, LeonardoService.LeonardoAction<T> action)
      throws LeonardoServiceException {

    return retryTemplate.execute(
        context -> {
          try {
            return action.execute();
          } catch (org.broadinstitute.dsde.workbench.client.leonardo.ApiException e) {
            throw new LeonardoServiceApiException(e);
          }
        });
  }
}
