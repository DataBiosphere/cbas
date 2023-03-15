package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "leonardo")
public record LeonardoServerConfig(String baseUri, String wdsAppTypeName) {
}
