package bio.terra.cbas.config;

import java.time.Duration;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "leonardo")
public class LeonardoServerConfiguration {

  private String baseUri;
  private List<AppType> wdsAppTypes;
  private Duration dependencyUrlCacheTtl;

  private static final Logger log = LoggerFactory.getLogger(LeonardoServerConfiguration.class);

  /** Allowable WDS app types, in preference order */
  public List<AppType> wdsAppTypes() {
    return wdsAppTypes;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    log.info("Setting baseUri=%s".formatted(baseUri));
    this.baseUri = baseUri;
  }

  public LeonardoServerConfiguration baseUri(String baseUri) {
    this.setBaseUri(baseUri);
    return this;
  }

  public void setWdsAppTypeNames(List<String> wdsAppTypeNames) {
    var transformed = wdsAppTypeNames.stream().map(AppType::fromValue).toList();
    log.info("Setting wdsAppTypes=%s".formatted(transformed));
    this.wdsAppTypes = transformed;
  }

  public LeonardoServerConfiguration wdsAppTypeNames(List<String> wdsAppTypeNames) {
    this.setWdsAppTypeNames(wdsAppTypeNames);
    return this;
  }

  public Duration getDependencyUrlCacheTtl() {
    return dependencyUrlCacheTtl;
  }

  public void setDependencyUrlCacheTtlSeconds(long dependencyUrlCacheTtlSeconds) {
    var transformed = Duration.ofSeconds(dependencyUrlCacheTtlSeconds);
    log.info("Setting dependencyUrlCacheTtl=%s".formatted(transformed));
    this.dependencyUrlCacheTtl = transformed;
  }
}
