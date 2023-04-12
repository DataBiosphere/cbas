package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import org.databiosphere.workspacedata.api.GeneralWdsInformationApi;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class WdsClient {

  private final WdsServerConfiguration wdsServerConfiguration;
  private final DependencyUrlLoader dependencyUrlLoader;

  public WdsClient(
      WdsServerConfiguration wdsServerConfiguration, DependencyUrlLoader dependencyUrlLoader) {
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.dependencyUrlLoader = dependencyUrlLoader;
  }

  private ApiClient getApiClient() throws DependencyNotAvailableException {
    String uri;
    if (wdsServerConfiguration.getBaseUri().isPresent()) {
      uri = wdsServerConfiguration.getBaseUri().get();
    } else {
      uri = dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL);
    }
    return new ApiClient().setBasePath(uri);
  }

  RecordsApi recordsApi() throws DependencyNotAvailableException {
    return new RecordsApi(getApiClient());
  }

  GeneralWdsInformationApi generalWdsInformationApi() throws DependencyNotAvailableException {
    return new GeneralWdsInformationApi(getApiClient());
  }
}
