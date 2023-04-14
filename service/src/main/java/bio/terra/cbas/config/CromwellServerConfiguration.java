package bio.terra.cbas.config;

import cromwell.client.ApiClient;
import cromwell.client.api.WomtoolApi;
import cromwell.client.api.WorkflowsApi;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflow-engines.cromwell")
public record CromwellServerConfiguration(String baseUri, String finalWorkflowLogDir) {
  public WorkflowsApi workflowsApi() {
    ApiClient client = new ApiClient();
    client.setBasePath(baseUri);
    return new WorkflowsApi(client);
  }

  public WomtoolApi womtoolApi() {
    ApiClient client = new ApiClient();
    client.setBasePath(baseUri);
    return new WomtoolApi(client);
  }
}
