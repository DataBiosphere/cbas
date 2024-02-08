package bio.terra.cbas.dependencies.wes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import cromwell.client.ApiClient;
import cromwell.client.api.EngineApi;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import cromwell.client.api.WomtoolApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestCromwellClient {
  @Mock DependencyUrlLoader dependencyUrlLoader;
  @Mock cromwell.client.ApiClient cromwellAuthReadClient;
  @Mock cromwell.client.ApiClient cromwellWriteClient;

  @Test
  void useConfiguredUrlIfAvailable() {
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(
            "http://localhost:8000/cromwell", null, "workflow/log/dir", false);

    CromwellClient cromwellClient =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader);
    ApiClient mockApiClient = cromwellClient.getReadApiClient();

    EngineApi engineApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader)
            .engineApi(mockApiClient);

    assertEquals("http://localhost:8000/cromwell", engineApi.getApiClient().getBasePath());
  }

  @Test
  void useConfiguredUrlIfAvailableWithToken() {
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(
            "http://localhost:8000/cromwell", false, "workflow/log/dir", false);

    ApiClient mockApiClient = cromwellAuthReadClient;
    when(mockApiClient.getBasePath()).thenReturn(cromwellServerConfiguration.baseUri());

    WomtoolApi womtoolApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader)
            .womtoolApi(mockApiClient);

    assertEquals("http://localhost:8000/cromwell", womtoolApi.getApiClient().getBasePath());
  }

  @Test
  void lookupCromwellUrlWhenNecessary() {
    String cromwellUri = "https://my-cromwell-service:10101/cromwell";
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(null, true, "workflow/log/dir", false);

    ApiClient mockApiClient = cromwellWriteClient;

    when(mockApiClient.getBasePath()).thenReturn(cromwellUri);

    Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader).wesAPI(mockApiClient);

    assertEquals("https://my-cromwell-service:10101/cromwell", wesApi.getApiClient().getBasePath());
  }
}
