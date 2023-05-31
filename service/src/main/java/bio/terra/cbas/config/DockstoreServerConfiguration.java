package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dockstore")
public record DockstoreServerConfiguration(String baseUri) {}
