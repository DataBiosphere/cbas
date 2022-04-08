package bio.terra.cbas;

import bio.terra.common.logging.LoggingInitializer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = {
      "bio.terra.cbas",
      "bio.terra.common.logging",
      "bio.terra.common.retry.transaction",
      "bio.terra.common.tracing"
    },
    excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class))
public class BatchAnalysisWebApplication {

  public static void main(String[] args) {
    new SpringApplicationBuilder(BatchAnalysisWebApplication.class)
        .initializers(new LoggingInitializer())
        .run(args);
  }
}
