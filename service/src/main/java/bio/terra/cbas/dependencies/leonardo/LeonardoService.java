package bio.terra.cbas.dependencies.leonardo;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheckable;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.api.AppsV2Api;
import org.broadinstitute.dsde.workbench.client.leonardo.api.ServiceInfoApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.SystemStatus;
import org.springframework.stereotype.Component;

@Component
public class LeonardoService implements HealthCheckable {

  private final LeonardoClient leonardoClient;

  private final WdsServerConfiguration wdsServerConfiguration;

  public LeonardoService(
      LeonardoClient leonardoClient, WdsServerConfiguration wdsServerConfiguration) {
    this.leonardoClient = leonardoClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  private AppsV2Api getAppsV2Api() throws AzureAccessTokenException {
    return new AppsV2Api(leonardoClient.getApiClient());
  }

  private ServiceInfoApi getServiceInfoApi() throws AzureAccessTokenException {
    return new ServiceInfoApi(leonardoClient.getApiClient());
  }

  public List<ListAppResponse> getApps() throws ApiException, AzureAccessTokenException {
    return getAppsV2Api().listAppsV2(wdsServerConfiguration.getInstanceId(), null, null, null);
  }

  @Override
  public HealthCheckResult checkHealth() {
    try {
      SystemStatus result = getServiceInfoApi().getSystemStatus();
      return new HealthCheckResult(result.getOk(), result.toString());
    } catch (ApiException | AzureAccessTokenException e) {
      return new HealthCheckResult(false, e.getMessage());
    }
  }
}
