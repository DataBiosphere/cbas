package bio.terra.cbas.dependencies.dockstore;

import bio.terra.cbas.config.DockstoreServerConfiguration;
import bio.terra.dockstore.client.ApiClient;
import jakarta.ws.rs.client.Client;
import org.springframework.stereotype.Component;

@Component
public class DockstoreClient {

  private final DockstoreServerConfiguration dockstoreServerConfiguration;
  private final Client singletonHttpClient;
  private static final String API_PREFIX = "api";

  public DockstoreClient(DockstoreServerConfiguration dockstoreServerConfiguration) {
    this.dockstoreServerConfiguration = dockstoreServerConfiguration;
    this.singletonHttpClient = new ApiClient().getHttpClient();
  }

  public ApiClient getApiClient() {
    return new ApiClient()
        .setHttpClient(singletonHttpClient)
        .addDefaultHeader("Connection", "close")
        .setBasePath(dockstoreServerConfiguration.baseUri() + API_PREFIX);
  }
}
