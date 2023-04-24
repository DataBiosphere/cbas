package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import java.util.Optional;
import org.databiosphere.workspacedata.api.GeneralWdsInformationApi;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestWdsClient {

  WdsServerConfiguration wdsServerConfiguration = mock(WdsServerConfiguration.class);
  DependencyUrlLoader dependencyUrlLoader = mock(DependencyUrlLoader.class);
  CredentialLoader credentialLoader = mock(CredentialLoader.class);

  @BeforeEach
  void setup() {
    wdsServerConfiguration = mock(WdsServerConfiguration.class);
    dependencyUrlLoader = mock(DependencyUrlLoader.class);
    credentialLoader = mock(CredentialLoader.class);
  }

  @Test
  void useConfiguredUrlIfAvailable() throws Exception {
    when(wdsServerConfiguration.getBaseUri()).thenReturn(Optional.of("http://localhost:8001/wds"));
    when(credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN))
        .thenReturn("TOKEN");

    RecordsApi recordsApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader, credentialLoader).recordsApi();

    verify(credentialLoader, times(1)).getCredential(CredentialLoader.CredentialType.AZURE_TOKEN);
    verify(dependencyUrlLoader, times(0)).loadDependencyUrl(any());
    assertEquals("http://localhost:8001/wds", recordsApi.getApiClient().getBasePath());
  }

  @Test
  void lookupWdsUrlWhenNecessary() throws Exception {
    when(wdsServerConfiguration.getBaseUri()).thenReturn(Optional.empty());
    when(credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN))
        .thenReturn("TOKEN");
    when(dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL))
        .thenReturn("https://my-wds-service:10101/wds");

    GeneralWdsInformationApi generalWdsInformationApi =
        new WdsClient(wdsServerConfiguration, dependencyUrlLoader, credentialLoader)
            .generalWdsInformationApi();

    verify(dependencyUrlLoader, times(1))
        .loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL);
    verify(credentialLoader, times(1)).getCredential(CredentialLoader.CredentialType.AZURE_TOKEN);
    assertEquals(
        "https://my-wds-service:10101/wds", generalWdsInformationApi.getApiClient().getBasePath());
  }
}
