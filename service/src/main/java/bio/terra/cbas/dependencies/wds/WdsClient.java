package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.config.WdsServerConfiguration;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class WdsClient {

  private final WdsServerConfiguration wdsServerConfiguration;

  public WdsClient(WdsServerConfiguration wdsServerConfiguration) {
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  private ApiClient getApiClient() {
    return new ApiClient().setBasePath(wdsServerConfiguration.getWdsProxyUrlRoot());
  }

  RecordsApi recordsApi() {
    return new RecordsApi(getApiClient());
  }
}
