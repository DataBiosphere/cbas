package bio.terra.cbas.dependencies.leonardo;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.iam.BearerToken;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.api.AppsApi;
import org.broadinstitute.dsde.workbench.client.leonardo.api.ServiceInfoApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.SystemStatus;
import org.springframework.stereotype.Component;

@Component
public class LeonardoService implements HealthCheck {

  private final LeonardoClient leonardoClient;
  private final BearerToken bearerToken;

  private final WdsServerConfiguration wdsServerConfiguration;

  public LeonardoService(
      LeonardoClient leonardoClient,
      WdsServerConfiguration wdsServerConfiguration,
      BearerToken bearerToken) {
    this.leonardoClient = leonardoClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.bearerToken = bearerToken;
  }

  private AppsApi getAppsApi() throws AzureAccessTokenException {
    return new AppsApi(leonardoClient.getApiClient(bearerToken.getToken()));
  }

  private ServiceInfoApi getServiceInfoApi() throws AzureAccessTokenException {
    return new ServiceInfoApi(leonardoClient.getApiClient());
  }

  public List<ListAppResponse> getApps() throws ApiException, AzureAccessTokenException {
    return getAppsApi().listAppsV2(wdsServerConfiguration.instanceId(), null, null, null);
  }

  @Override
  public Result checkHealth() {
    try {
      SystemStatus result = getServiceInfoApi().getSystemStatus();
      return new Result(result.getOk(), result.toString());
    } catch (ApiException | AzureAccessTokenException e) {
      return new Result(false, e.getMessage());
    }
  }
}
