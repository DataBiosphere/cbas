package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wds")
public record WdsServerConfiguration(String baseUri, String healthcheckEndpoint) {
  public String healthcheckUri() {
    return "%s/%s".formatted(baseUri, healthcheckEndpoint);
  }
}
