package bio.terra.cbas.config;

import bio.terra.cbas.common.DurationUtils;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "credentials.azure")
public class AzureCredentialConfig {

  private Duration tokenAcquisitionTimeout;
  private Duration tokenCacheTtl;
  private Optional<String> manualTokenOverride;

  public Duration getTokenAcquisitionTimeout() {
    return tokenAcquisitionTimeout;
  }

  public void setTokenAcquisitionTimeout(String tokenAcquisitionTimeout) {
    this.tokenAcquisitionTimeout = DurationUtils.durationFromString(tokenAcquisitionTimeout);
  }

  public Duration getTokenCacheTtl() {
    return tokenCacheTtl;
  }

  public void setTokenCacheTtl(String tokenCacheTtl) {
    this.tokenCacheTtl = DurationUtils.durationFromString(tokenCacheTtl);
  }

  public Optional<String> getManualTokenOverride() {
    return manualTokenOverride;
  }

  public void setManualTokenOverride(String manualTokenOverride) {
    this.manualTokenOverride = Optional.of(manualTokenOverride);
  }
}
