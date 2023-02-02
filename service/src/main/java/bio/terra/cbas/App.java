package bio.terra.cbas;

import bio.terra.cbas.common.MetricsUtil;
import bio.terra.common.logging.LoggingInitializer;
import javax.sql.DataSource;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
    scanBasePackages = {
      // Scan for logging-related components & configs
      "bio.terra.common.logging",
      // Scan for Liquibase migration components & configs
      "bio.terra.common.migrate",
      // Transaction management and DB retry configuration
      "bio.terra.common.retry.transaction",
      // Scan for tracing-related components & configs
      "bio.terra.common.tracing",
      // Scan all service-specific packages beneath the current package
      "bio.terra.cbas",
      // Metrics exporting components & configs
      "bio.terra.common.prometheus"
    })
@ConfigurationPropertiesScan("bio.terra.cbas")
@EnableRetry
@EnableTransactionManagement
@SpringBootConfiguration
public class App {

  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource cbasDb() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  @Primary
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
    return new NamedParameterJdbcTemplate(cbasDb());
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(App.class).initializers(new LoggingInitializer()).run(args);
    MetricsUtil.registerAllViews();
  }
}
