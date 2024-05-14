package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bard")
public record BardServerConfiguration(String baseUri, Boolean debugApiLogging) {}
