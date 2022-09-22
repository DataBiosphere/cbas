package bio.terra.cbas.config;

import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiClient;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wds")
public record WdsServerConfiguration(
    String baseUri, String healthcheckEndpoint, String instanceId, String apiV) {
  public String healthcheckUri() {
    return "%s/%s".formatted(baseUri, healthcheckEndpoint);
  }

  public RecordsApi recordsApi() {
    ApiClient wdsApiClient = new ApiClient();
    wdsApiClient.setBasePath(baseUri);

    return new RecordsApi(wdsApiClient);
  }
}
