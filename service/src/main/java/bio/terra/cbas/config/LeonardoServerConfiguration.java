package bio.terra.cbas.config;

import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "leonardo")
public record LeonardoServerConfiguration(String baseUri, List<String> wdsAppTypeNames) {

  /** Allowable WDS app types, in preference order */
  public List<AppType> wdsAppTypes() {
    return wdsAppTypeNames.stream().map(AppType::fromValue).toList();
  }
}
