package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "workflow-engines.cromwell")
public record CromwellServerConfiguration(String baseUri, String healthUri) {}
