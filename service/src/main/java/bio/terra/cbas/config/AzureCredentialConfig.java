package bio.terra.cbas.config;

import bio.terra.cbas.common.DurationUtils;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "credentials.azure")
public class AzureCredentialConfig {

  private final Duration tokenAcquisitionTimeout;
  private final Duration tokenCacheTtl;
  private final Optional<String> manualTokenOverride;

  public AzureCredentialConfig(
      String tokenAcquisitionTimeout, String tokenCacheTtl, Optional<String> manualTokenOverride) {
    this.tokenAcquisitionTimeout = DurationUtils.durationFromString(tokenAcquisitionTimeout);
    this.tokenCacheTtl = DurationUtils.durationFromString(tokenCacheTtl);
    this.manualTokenOverride = manualTokenOverride;
  }

  public Duration getTokenAcquisitionTimeout() {
    return tokenAcquisitionTimeout;
  }

  public Duration getTokenCacheTtl() {
    return tokenCacheTtl;
  }

  public Optional<String> getManualTokenOverride() {
    return manualTokenOverride;
  }
}
