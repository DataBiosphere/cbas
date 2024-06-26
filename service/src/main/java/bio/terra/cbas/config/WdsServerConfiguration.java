package bio.terra.cbas.config;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wds")
public record WdsServerConfiguration(
    String baseUri,
    String instanceId,
    String apiV,
    Integer queryWindowSize,
    Boolean debugApiLogging) {
  public Optional<String> getBaseUri() {
    return Optional.ofNullable(baseUri);
  }
}
