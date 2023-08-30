package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import bio.terra.common.iam.BearerToken;
import org.databiosphere.workspacedata.api.GeneralWdsInformationApi;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestWdsClient {

  @Mock DependencyUrlLoader dependencyUrlLoader;
  @Mock CredentialLoader credentialLoader;
  @Mock BearerToken bearerToken;

  @Test
  void useConfiguredUrlIfAvailable() throws Exception {
    WdsServerConfiguration wdsServerConfiguration =
        new WdsServerConfiguration("http://localhost:8001/wds", "instanceId", "apiV", false);

    when(credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN))
        .thenReturn("TOKEN");

    RecordsApi recordsApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader, credentialLoader)
            .recordsApi(bearerToken.getToken());

    assertEquals("http://localhost:8001/wds", recordsApi.getApiClient().getBasePath());
  }

  @Test
  void lookupWdsUrlWhenNecessary() throws Exception {
    WdsServerConfiguration wdsServerConfiguration =
        new WdsServerConfiguration(null, "instanceId", "apiV", false);
    when(credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN))
        .thenReturn("TOKEN");
    when(dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL))
        .thenReturn("https://my-wds-service:10101/wds");

    GeneralWdsInformationApi generalWdsInformationApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader, credentialLoader)
            .generalWdsInformationApi();

    assertEquals(
        "https://my-wds-service:10101/wds", generalWdsInformationApi.getApiClient().getBasePath());
  }
}
