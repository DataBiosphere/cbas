package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import cromwell.client.ApiClient;
import cromwell.client.api.WorkflowsApi;

@ConfigurationProperties(prefix = "workflow-engines.cromwell")
public record CromwellServerConfiguration(String baseUri, String healthUri, String finalWorkflowLogDir) {
  public WorkflowsApi workflowsApi() {
    ApiClient client = new ApiClient();
    client.setBasePath(baseUri);
    return new WorkflowsApi(client);
  }
}
