package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.config.SamServerConfiguration;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class SamClient {

  private final SamServerConfiguration samServerConfiguration;
  private final OkHttpClient singletonHttpClient;

  public SamClient(SamServerConfiguration samServerConfiguration) {
    this.samServerConfiguration = samServerConfiguration;
    this.singletonHttpClient =
        new ApiClient().getHttpClient().newBuilder().protocols(List.of(Protocol.HTTP_1_1)).build();
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
