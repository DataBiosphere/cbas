package bio.terra.cbas.config;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wds")
public record WdsServerConfiguration(
    String baseUri, String healthcheckEndpoint, String instanceId, String apiV) {
  public Optional<String> getBaseUri() {
    return Optional.ofNullable(baseUri);
  }
}
