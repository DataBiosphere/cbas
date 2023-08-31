package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflow-engines.cromwell")
public record CromwellServerConfiguration(
    String baseUri,
    Boolean fetchCromwellUrlFromLeo,
    String finalWorkflowLogDir,
    Boolean debugApiLogging) {}
