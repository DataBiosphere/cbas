package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecm")
public record EcmServerConfiguration(String baseUri, Boolean debugApiLogging) {}
