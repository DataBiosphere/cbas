package bio.terra.cbas.config;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.dependencies.leonardo.AppUtils;
import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
@EnableScheduling
public class BeanConfig {

  @Bean("objectMapper")
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new ParameterNamesModule())
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .setDateFormat(new StdDateFormat())
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  /**
   * Taken from <a
   * href="https://github.com/DataBiosphere/terra-data-catalog/blob/5cda83aef8548ff98e7cfbe2a6eaaed9ad1bff45/common/src/main/java/bio/terra/catalog/config/BeanConfig.java#L34-L38">Terra
   * Data Catalog</a> Lasts for the duration of one request, and is injected into dependent beans,
   * even singletons
   */
  @Bean("bearerToken")
  @RequestScope
  public BearerToken bearerToken(HttpServletRequest request) {
    return new BearerTokenFactory().from(request);
  }

  @Bean("cromwellUri")
  @RequestScope
  public String cromwellUri(LeonardoService leonardoService, AppUtils appUtils)
      throws DependencyNotAvailableException {
    try {
      List<ListAppResponse> allApps = leonardoService.getApps();
      return appUtils.findUrlForCromwell(allApps);
    } catch (ApiException | AzureAccessTokenException e) {
      throw new DependencyNotAvailableException("Cromwell", "Failed to poll Leonardo for URL", e);
    }
  }
}
