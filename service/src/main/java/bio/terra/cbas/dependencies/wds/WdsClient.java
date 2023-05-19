package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import java.util.Optional;
import org.databiosphere.workspacedata.api.GeneralWdsInformationApi;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class WdsClient {

  private final WdsServerConfiguration wdsServerConfiguration;
  private final DependencyUrlLoader dependencyUrlLoader;

  private final CredentialLoader credentialLoader;

  private final ScheduledWdsClientRefresher scheduledWdsClientRefresher;

  public WdsClient(
      WdsServerConfiguration wdsServerConfiguration,
      DependencyUrlLoader dependencyUrlLoader,
      CredentialLoader credentialLoader,
      ScheduledWdsClientRefresher scheduledWdsClientRefresher) {
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.dependencyUrlLoader = dependencyUrlLoader;
    this.credentialLoader = credentialLoader;
    this.scheduledWdsClientRefresher = scheduledWdsClientRefresher;
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
    apiClient.addDefaultHeader(
        "Authorization",
        "Bearer " + credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    apiClient.setDebugging(wdsServerConfiguration.debugApiLogging());
    apiClient.setHttpClient(scheduledWdsClientRefresher.getCurrentClient());
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
