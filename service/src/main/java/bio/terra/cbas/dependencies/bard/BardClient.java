package bio.terra.cbas.dependencies.bard;

import bio.terra.bard.api.DefaultApi;
import bio.terra.bard.client.ApiClient;
import bio.terra.cbas.config.BardServerConfiguration;
import bio.terra.common.iam.BearerToken;
import org.springframework.stereotype.Component;

@Component
public class BardClient {

  private final BardServerConfiguration bardServerConfiguration;

  public BardClient(BardServerConfiguration bardServerConfiguration) {
    this.bardServerConfiguration = bardServerConfiguration;
  }

  public ApiClient apiClient() {
    ApiClient apiClient = new ApiClient();

    apiClient.setBasePath(bardServerConfiguration.baseUri());
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(bardServerConfiguration.debugApiLogging());

    return apiClient;
  }

  public ApiClient bardAuthClient(BearerToken userToken) {
    ApiClient apiClient = new ApiClient();

    apiClient.setBasePath(bardServerConfiguration.baseUri());
    apiClient.setBearerToken(userToken.getToken());
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(bardServerConfiguration.debugApiLogging());

    return apiClient;
  }

  public DefaultApi defaultApi(ApiClient apiClient) {
    return new DefaultApi(apiClient);
  }
}
