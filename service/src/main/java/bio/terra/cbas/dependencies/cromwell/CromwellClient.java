package bio.terra.cbas.dependencies.cromwell;

import bio.terra.cbas.config.CromwellServerConfiguration;
import cromwell.client.ApiClient;
import cromwell.client.api.WorkflowsApi;
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

  WorkflowsApi workflowsApi() {
    return new WorkflowsApi(getApiClient());
  }
}
