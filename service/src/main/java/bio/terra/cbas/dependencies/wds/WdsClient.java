package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import java.util.Optional;
import okhttp3.OkHttpClient;
import org.databiosphere.workspacedata.api.GeneralWdsInformationApi;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class WdsClient {

  private final WdsServerConfiguration wdsServerConfiguration;
  private final DependencyUrlLoader dependencyUrlLoader;

  private final CredentialLoader credentialLoader;

  private final OkHttpClient singletonHttpClient;

  public WdsClient(
      WdsServerConfiguration wdsServerConfiguration,
      DependencyUrlLoader dependencyUrlLoader,
      CredentialLoader credentialLoader) {
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.dependencyUrlLoader = dependencyUrlLoader;
    this.credentialLoader = credentialLoader;
    singletonHttpClient = new ApiClient().getHttpClient().newBuilder().build();
  }

  protected ApiClient getApiClient()
      throws DependencyNotAvailableException, AzureAccessTokenException {

    String uri;

    Optional<String> baseUriFromConfig = wdsServerConfiguration.getBaseUri();
    if (baseUriFromConfig.isPresent()) {
      uri = baseUriFromConfig.get();
    } else {
      uri = dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL);
    }

    ApiClient apiClient = new ApiClient().setBasePath(uri);
    apiClient.setHttpClient(singletonHttpClient);
    apiClient.addDefaultHeader(
        "Authorization",
        "Bearer " + credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    // By closing the connection after each request, we avoid the problem of the open connection
    // being force-closed ungracefully by the Azure Relay/Listener infrastructure:
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(wdsServerConfiguration.debugApiLogging());
    return apiClient;
  }

  RecordsApi recordsApi() throws DependencyNotAvailableException, AzureAccessTokenException {
    return new RecordsApi(getApiClient());
  }

  GeneralWdsInformationApi generalWdsInformationApi()
      throws DependencyNotAvailableException, AzureAccessTokenException {
    return new GeneralWdsInformationApi(getApiClient());
  }
}
