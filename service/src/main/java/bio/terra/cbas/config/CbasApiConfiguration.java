package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cbas.cbas-api")
public record CbasApiConfiguration(int runSetsMaximumRecordIds) {}
