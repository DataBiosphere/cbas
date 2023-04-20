package bio.terra.cbas.config;

import bio.terra.cbas.common.DurationUtils;
import java.time.Duration;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "leonardo")
public class LeonardoServerConfiguration {

  private String baseUri;
  private List<String> wdsAppTypeNames;
  private Duration dependencyUrlCacheTtl;

  /** Allowable WDS app types, in preference order */
  public List<AppType> wdsAppTypes() {
    return wdsAppTypeNames.stream().map(AppType::fromValue).toList();
  }

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public LeonardoServerConfiguration baseUri(String baseUri) {
    this.setBaseUri(baseUri);
    return this;
  }

  public void setWdsAppTypeNames(List<String> wdsAppTypeNames) {
    this.wdsAppTypeNames = wdsAppTypeNames;
  }

  public LeonardoServerConfiguration wdsAppTypeNames(List<String> wdsAppTypeNames) {
    this.setWdsAppTypeNames(wdsAppTypeNames);
    return this;
  }

  public Duration getDependencyUrlCacheTtl() {
    return dependencyUrlCacheTtl;
  }

  public void setDependencyUrlCacheTtl(String dependencyUrlCacheTtl) {
    this.dependencyUrlCacheTtl = DurationUtils.durationFromString(dependencyUrlCacheTtl);
  }
}
