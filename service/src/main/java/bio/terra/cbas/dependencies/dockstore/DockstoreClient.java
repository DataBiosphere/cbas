package bio.terra.cbas.dependencies.dockstore;

import bio.terra.cbas.config.DockstoreServerConfiguration;
import bio.terra.dockstore.client.ApiClient;
import javax.ws.rs.client.Client;
import org.springframework.stereotype.Component;

@Component
public class DockstoreClient {

  private final DockstoreServerConfiguration dockstoreServerConfiguration;
  private final Client commonHttpClient = new ApiClient().getHttpClient();

  public DockstoreClient(DockstoreServerConfiguration dockstoreServerConfiguration) {
    this.dockstoreServerConfiguration = dockstoreServerConfiguration;
  }

  public ApiClient getApiClient() {
    return new ApiClient()
        .setHttpClient(commonHttpClient)
        .setBasePath(dockstoreServerConfiguration.getBaseUri());
  }
}
