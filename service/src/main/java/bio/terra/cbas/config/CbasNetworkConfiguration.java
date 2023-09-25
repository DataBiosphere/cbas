package bio.terra.cbas.config;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cbas.network")
public class CbasNetworkConfiguration {

  private String externalUri;

  public Optional<String> getCallbackUri() {
    if (externalUri == null || externalUri.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(externalUri + "api/batch/v1/runs/results");
    }
  }

  public String getExternalUri() {
    return externalUri;
  }

  public void setExternalUri(String externalUri) {
    this.externalUri = externalUri;
  }
}
