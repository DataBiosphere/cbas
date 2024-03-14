package bio.terra.cbas.dependencies.ecm;

import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.iam.BearerToken;
import bio.terra.externalcreds.client.ApiClient;
import bio.terra.externalcreds.model.Provider;
import bio.terra.externalcreds.model.SystemStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class EcmService implements HealthCheck {

  private final EcmClient ecmClient;
  private final BearerToken bearerToken;

  public EcmService(EcmClient ecmClient, BearerToken bearerToken) {
    this.ecmClient = ecmClient;
    this.bearerToken = bearerToken;
  }

  public String getAccessToken() {
    ApiClient client = ecmClient.ecmAuthClient(bearerToken.getToken());
    return ecmClient.oAuthApi(client).getProviderAccessToken(Provider.GITHUB);
  }

  @Override
  public Result checkHealth() {
    try {
      ApiClient client = ecmClient.apiClient();
      SystemStatus status = ecmClient.publicApi(client).getStatus();
      return new Result(status.isOk(), status.toString());
    } catch (RestClientException e) {
      return new Result(false, e.getMessage());
    }
  }
}
