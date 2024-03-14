package bio.terra.cbas.dependencies.ecm;

import bio.terra.cbas.config.EcmServerConfiguration;
import bio.terra.externalcreds.api.OauthApi;
import bio.terra.externalcreds.api.PublicApi;
import bio.terra.externalcreds.client.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class EcmClient {
  private final EcmServerConfiguration ecmServerConfiguration;

  public EcmClient(EcmServerConfiguration ecmServerConfiguration) {
    this.ecmServerConfiguration = ecmServerConfiguration;
  }

  public ApiClient apiClient() {
    ApiClient apiClient = new ApiClient();

    apiClient.setBasePath(ecmServerConfiguration.baseUri());
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(ecmServerConfiguration.debugApiLogging());

    return apiClient;
  }

  public ApiClient ecmAuthClient(String accessToken) {
    ApiClient apiClient = new ApiClient();

    apiClient.setBasePath(ecmServerConfiguration.baseUri());
    apiClient.setAccessToken(accessToken);
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(ecmServerConfiguration.debugApiLogging());

    return apiClient;
  }

  public OauthApi oAuthApi(ApiClient apiClient) {
    return new OauthApi(apiClient);
  }

  public PublicApi publicApi(ApiClient apiClient) {
    return new PublicApi(apiClient);
  }
}
