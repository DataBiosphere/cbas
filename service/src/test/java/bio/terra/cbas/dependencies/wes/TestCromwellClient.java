package bio.terra.cbas.dependencies.wes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import cromwell.client.api.EngineApi;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestCromwellClient {

  @Mock DependencyUrlLoader dependencyUrlLoader;
  @Mock CredentialLoader credentialLoader;

  @Test
  void useConfiguredUrlIfAvailable() throws Exception {
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(
            "http://localhost:8000/cromwell", "workflow/log/dir", false);

    when(credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN))
        .thenReturn("TOKEN");

    EngineApi engineApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader, credentialLoader)
            .engineApi();

    assertEquals("http://localhost:8000/cromwell", engineApi.getApiClient().getBasePath());
  }

  @Test
  void lookupCromwellUrlWhenNecessary() throws Exception {
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(null, "workflow/log/dir", false);
    when(credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN))
        .thenReturn("TOKEN");
    when(dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.CROMWELL_URL))
        .thenReturn("https://my-cromwell-service:10101/cromwell");

    Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader, credentialLoader)
            .wesAPI();

    assertEquals("https://my-cromwell-service:10101/cromwell", wesApi.getApiClient().getBasePath());
  }
}
