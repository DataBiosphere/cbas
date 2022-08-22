package bio.terra.cbas.config;

import cromwell.client.ApiClient;
import cromwell.client.api.WorkflowsApi;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflow-engines.cromwell")
public record CromwellServerConfiguration(String baseUri, String healthUri) {
  public WorkflowsApi workflowsApi() {
    ApiClient client = new ApiClient();
    client.setBasePath(baseUri);
    return new WorkflowsApi(client);
  }
}
