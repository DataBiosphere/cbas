package bio.terra.cbas.config;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "credentials.azure")
public record AzureCredentialConfig(
    Duration tokenAcquisitionTimeout, Duration tokenCacheTtl, String manualTokenOverride) {
  private static final Logger log = LoggerFactory.getLogger(AzureCredentialConfig.class);

  @ConstructorBinding
  public AzureCredentialConfig(
      long tokenAcquisitionTimeoutSeconds, long tokenCacheTtlSeconds, String manualTokenOverride) {
    this(
        Duration.ofSeconds(tokenAcquisitionTimeoutSeconds),
        Duration.ofSeconds(tokenCacheTtlSeconds),
        manualTokenOverride);
    log.info("Setting tokenAcquisitionTimeout={}", tokenAcquisitionTimeout);
    log.info("Setting tokenCacheTtl={}", tokenCacheTtl);
    if (log.isInfoEnabled()) {
      log.info("Setting manualTokenOverride={}", manualTokenOverride != null ? "(set)" : "(empty)");
    }
  }

  public Optional<String> getManualTokenOverride() {
    return Optional.ofNullable(manualTokenOverride);
  }
}
