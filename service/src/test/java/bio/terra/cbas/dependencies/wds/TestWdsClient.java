package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestWdsClient {

  @Mock DependencyUrlLoader dependencyUrlLoader;

  @Test
  void useConfiguredUrlIfAvailable() throws Exception {
    WdsServerConfiguration wdsServerConfiguration =
        new WdsServerConfiguration("http://localhost:8001/wds", "instanceId", "apiV", false);

    RecordsApi recordsApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader).recordsApi("TOKEN");

    assertEquals("http://localhost:8001/wds", recordsApi.getApiClient().getBasePath());
  }

  @Test
  void lookupWdsUrlWhenNecessary() throws Exception {
    WdsServerConfiguration wdsServerConfiguration =
        new WdsServerConfiguration(null, "instanceId", "apiV", false);
    when(dependencyUrlLoader.loadDependencyUrl(
            eq(DependencyUrlLoader.DependencyUrlType.WDS_URL), any()))
        .thenReturn("https://my-wds-service:10101/wds");

    RecordsApi recordsApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader).recordsApi("TOKEN");

    assertEquals("https://my-wds-service:10101/wds", recordsApi.getApiClient().getBasePath());
  }
}
