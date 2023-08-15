package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.config.SamServerConfiguration;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class SamClient {

  private final SamServerConfiguration samServerConfiguration;
  private final OkHttpClient singletonHttpClient;

  public SamClient(SamServerConfiguration samServerConfiguration) {
    this.samServerConfiguration = samServerConfiguration;
    this.singletonHttpClient = new ApiClient().getHttpClient();
  }

  public ApiClient getApiClient() {
    return new ApiClient()
        .setHttpClient(singletonHttpClient)
        .addDefaultHeader("Connection", "close")
        .setBasePath(samServerConfiguration.baseUri())
        .setDebugging(samServerConfiguration.debugApiLogging());
  }

  public ApiClient getApiClient(String accessToken) {
    ApiClient apiClient = getApiClient();
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }

  public String getWorkspaceId() {
    return this.samServerConfiguration.workspaceId();
  }

  public boolean checkAuthAccessWithSam() {
    return this.samServerConfiguration.checkAuthAccess();
  }
}
