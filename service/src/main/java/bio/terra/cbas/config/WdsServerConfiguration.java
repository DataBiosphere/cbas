package bio.terra.cbas.config;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wds")
public class WdsServerConfiguration {

  private String baseUri;
  private String healthcheckEndpoint;
  private String instanceId;
  private String apiV;

  public Optional<String> getBaseUri() {
    return Optional.ofNullable(baseUri);
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
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
}
