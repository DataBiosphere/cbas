package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sam")
public record SamServerConfiguration(String baseUri, Boolean debugApiLogging) {}
