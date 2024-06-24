package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import bio.terra.common.iam.BearerToken;
import org.databiosphere.workspacedata.api.CapabilitiesApi;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestWdsClient {

  @Mock DependencyUrlLoader dependencyUrlLoader;

  private final BearerToken userToken = new BearerToken("TOKEN");

  @Test
  void useConfiguredUrlIfAvailable() throws Exception {
    WdsServerConfiguration wdsServerConfiguration =
        new WdsServerConfiguration("http://localhost:8001/wds", "instanceId", "apiV", 1000, false);

    RecordsApi recordsApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader).recordsApi(userToken);

    CapabilitiesApi capabilitiesApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader).capabilitiesApi(userToken);

    assertEquals("http://localhost:8001/wds", recordsApi.getApiClient().getBasePath());
    assertEquals("http://localhost:8001/wds", capabilitiesApi.getApiClient().getBasePath());
  }

  @Test
  void lookupWdsUrlWhenNecessary() throws Exception {
    WdsServerConfiguration wdsServerConfiguration =
        new WdsServerConfiguration(null, "instanceId", "apiV", 1000, false);
    when(dependencyUrlLoader.loadDependencyUrl(
            eq(DependencyUrlLoader.DependencyUrlType.WDS_URL), any()))
        .thenReturn("https://my-wds-service:10101/wds");

    RecordsApi recordsApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader).recordsApi(userToken);

    CapabilitiesApi capabilitiesApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader).capabilitiesApi(userToken);

    assertEquals("https://my-wds-service:10101/wds", recordsApi.getApiClient().getBasePath());
    assertEquals("https://my-wds-service:10101/wds", capabilitiesApi.getApiClient().getBasePath());
  }
}
