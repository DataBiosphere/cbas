package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cbas.scheduler")
public record CbasSchedulerConfiguration(int healthCheckIntervalSeconds) {}
