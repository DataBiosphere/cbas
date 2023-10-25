package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import java.util.Optional;
import okhttp3.OkHttpClient;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class WdsClient {

  private final WdsServerConfiguration wdsServerConfiguration;
  private final DependencyUrlLoader dependencyUrlLoader;

  private final OkHttpClient singletonHttpClient;

  public WdsClient(
      WdsServerConfiguration wdsServerConfiguration, DependencyUrlLoader dependencyUrlLoader) {
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.dependencyUrlLoader = dependencyUrlLoader;
    singletonHttpClient = new ApiClient().getHttpClient().newBuilder().build();
  }

  protected ApiClient getApiClient(String accessToken) throws DependencyNotAvailableException {

    String uri;

    Optional<String> baseUriFromConfig = wdsServerConfiguration.getBaseUri();
    if (baseUriFromConfig.isPresent()) {
      uri = baseUriFromConfig.get();
    } else {
      uri =
          dependencyUrlLoader.loadDependencyUrl(
              DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken);
    }

    ApiClient apiClient = new ApiClient().setBasePath(uri);
    apiClient.setHttpClient(singletonHttpClient);
    apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
    // By closing the connection after each request, we avoid the problem of the open connection
    // being force-closed ungracefully by the Azure Relay/Listener infrastructure:
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(wdsServerConfiguration.debugApiLogging());
    return apiClient;
  }

  RecordsApi recordsApi(String accessToken) throws DependencyNotAvailableException {
    return new RecordsApi(getApiClient(accessToken));
  }
}
