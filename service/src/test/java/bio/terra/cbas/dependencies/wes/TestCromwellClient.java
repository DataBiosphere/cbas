package bio.terra.cbas.dependencies.wes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import cromwell.client.ApiClient;
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
            "http://localhost:8000/cromwell", null, "workflow/log/dir", false);

    CromwellClient cromwellClient =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader, credentialLoader);
    ApiClient mockApiClient = cromwellClient.getReadApiClient();

    EngineApi engineApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader, credentialLoader)
            .engineApi(mockApiClient);

    assertEquals("http://localhost:8000/cromwell", engineApi.getApiClient().getBasePath());
  }

  @Test
  void lookupCromwellUrlWhenNecessary() throws Exception {
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(null, true, "workflow/log/dir", false);

    when(credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN))
        .thenReturn("TOKEN");
    when(dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.CROMWELL_URL))
        .thenReturn("https://my-cromwell-service:10101/cromwell");
    CromwellClient cromwellClient =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader, credentialLoader);

    ApiClient mockApiClient = cromwellClient.getWriteApiClient();

    Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader, credentialLoader)
            .wesAPI(mockApiClient);

    assertEquals("https://my-cromwell-service:10101/cromwell", wesApi.getApiClient().getBasePath());
  }
}
