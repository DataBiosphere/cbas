package bio.terra.cbas.config;

import java.time.Duration;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "leonardo")
public record LeonardoServerConfiguration(
    String baseUri,
    List<AppType> wdsAppTypeNames,
    List<AppType> cromwellRunnerAppTypeNames,
    Duration dependencyUrlCacheTtl,
    Boolean debugApiLogging) {

  private static final Logger log = LoggerFactory.getLogger(LeonardoServerConfiguration.class);

  @ConstructorBinding
  public LeonardoServerConfiguration(
      String baseUri,
      List<String> wdsAppTypeNames,
      List<String> cromwellRunnerAppTypeNames,
      long dependencyUrlCacheTtlSeconds,
      Boolean debugApiLogging) {
    this(
        baseUri,
        wdsAppTypeNames.stream().map(AppType::fromValue).toList(),
        cromwellRunnerAppTypeNames.stream().map(AppType::fromValue).toList(),
        Duration.ofSeconds(dependencyUrlCacheTtlSeconds),
        debugApiLogging);
    log.info("Setting wdsAppTypes={}", wdsAppTypeNames);
    log.info("Setting cromwellRunnerAppTypeNames={}", cromwellRunnerAppTypeNames);
  }
}
