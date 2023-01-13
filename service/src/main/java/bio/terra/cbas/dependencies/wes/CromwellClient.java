package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.config.CromwellServerConfiguration;
import cromwell.client.ApiClient;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CromwellClient {

  private final CromwellServerConfiguration cromwellServerConfiguration;

  public CromwellClient(CromwellServerConfiguration cromwellServerConfiguration) {
    this.cromwellServerConfiguration = cromwellServerConfiguration;
  }

  private ApiClient getApiClient() {
    return new ApiClient().setBasePath(cromwellServerConfiguration.baseUri());
  }

  public Optional<String> getFinalWorkflowLogDirOption() {
    return Optional.ofNullable(this.cromwellServerConfiguration.finalWorkflowLogDir());
  }

  Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesAPI() {
    return new Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi(getApiClient());
  }
}
