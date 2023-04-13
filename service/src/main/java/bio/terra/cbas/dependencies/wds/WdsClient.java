package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import org.databiosphere.workspacedata.api.GeneralWdsInformationApi;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class WdsClient {

  private final WdsServerConfiguration wdsServerConfiguration;
  private final DependencyUrlLoader dependencyUrlLoader;

  private final CredentialLoader credentialLoader;

  public WdsClient(
      WdsServerConfiguration wdsServerConfiguration,
      DependencyUrlLoader dependencyUrlLoader,
      CredentialLoader credentialLoader) {
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.dependencyUrlLoader = dependencyUrlLoader;
    this.credentialLoader = credentialLoader;
  }

  private ApiClient getApiClient()
      throws DependencyNotAvailableException, AzureAccessTokenException {
    String uri;
    if (wdsServerConfiguration.getBaseUri().isPresent()) {
      uri = wdsServerConfiguration.getBaseUri().get();
    } else {
      uri = dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL);
    }

    System.out.printf("Using WDS URI: %s%n", uri);

    return new ApiClient()
        .setBasePath(uri)
        .setDebugging(true)
        .addDefaultHeader(
            "Authorization",
            "Bearer %s"
                .formatted(
                    credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN)));
  }

  RecordsApi recordsApi() throws DependencyNotAvailableException, AzureAccessTokenException {
    return new RecordsApi(getApiClient());
  }

  GeneralWdsInformationApi generalWdsInformationApi()
      throws DependencyNotAvailableException, AzureAccessTokenException {
    return new GeneralWdsInformationApi(getApiClient());
  }
}
