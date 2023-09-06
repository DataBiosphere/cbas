package bio.terra.cbas.dependencies.wes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import bio.terra.common.iam.BearerToken;
import cromwell.client.ApiClient;
import cromwell.client.api.EngineApi;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestCromwellClient {
  @Mock BearerToken bearerToken;
  @Mock DependencyUrlLoader dependencyUrlLoader;

  @Test
  void useConfiguredUrlIfAvailable() throws Exception {
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(
            "http://localhost:8000/cromwell", null, "workflow/log/dir", false);

    CromwellClient cromwellClient =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader);
    ApiClient mockApiClient = cromwellClient.getReadApiClient(bearerToken.getToken());

    EngineApi engineApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader)
            .engineApi(mockApiClient);

    assertEquals("http://localhost:8000/cromwell", engineApi.getApiClient().getBasePath());
  }

  @Test
  void lookupCromwellUrlWhenNecessary() throws DependencyNotAvailableException {
    String cromwellUri = "https://my-cromwell-service:10101/cromwell";
    CromwellServerConfiguration cromwellServerConfiguration =
        new CromwellServerConfiguration(null, true, "workflow/log/dir", false);

    CromwellClient cromwellClient =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader);

    ApiClient mockApiClient = cromwellClient.getWriteApiClient(bearerToken.getToken());

    Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesApi =
        new CromwellClient(cromwellServerConfiguration, dependencyUrlLoader).wesAPI(mockApiClient);

    assertEquals("https://my-cromwell-service:10101/cromwell", wesApi.getApiClient().getBasePath());
  }
}
