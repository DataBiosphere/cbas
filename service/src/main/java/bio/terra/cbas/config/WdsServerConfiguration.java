package bio.terra.cbas.config;

import bio.terra.cbas.dependencies.common.AzureCredentials;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wds")
public class WdsServerConfiguration {

  private Optional<String> baseUri;
  private String healthcheckEndpoint;
  private String instanceId;
  private String apiV;

  public Optional<String> getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = Optional.ofNullable(baseUri);
  }

  public String getHealthcheckEndpoint() {
    return healthcheckEndpoint;
  }

  public void setHealthcheckEndpoint(String healthcheckEndpoint) {
    this.healthcheckEndpoint = healthcheckEndpoint;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public WdsServerConfiguration instanceId(String instanceId) {
    this.instanceId = instanceId;
    return this;
  }

  public String getApiV() {
    return apiV;
  }

  public void setApiV(String apiV) {
    this.apiV = apiV;
  }

  public String healthcheckUri() {
    System.out.printf("WDS baseUri: %s%n", this.baseUri);

    try {
      String token = new AzureCredentials().getAccessToken();
      System.out.printf("AzureCredentials: %s%n", token);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    return "%s/%s".formatted(this.baseUri, this.healthcheckEndpoint);
  }
}
