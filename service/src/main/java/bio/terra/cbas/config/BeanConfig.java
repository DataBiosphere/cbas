package bio.terra.cbas.config;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.dependencies.sam.SamClient;
import bio.terra.cbas.dependencies.wes.CromwellClient;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import javax.servlet.http.HttpServletRequest;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
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

  @Bean("userInfo")
  @RequestScope
  public UserStatusInfo userInfo(SamClient samClient, BearerToken bearerToken) {
    if (!samClient.checkAuthAccessWithSam()) {
      return new UserStatusInfo(); // Dummy user for local testing
    }
    UsersApi usersApi = new UsersApi(samClient.getApiClient(bearerToken.getToken()));
    try {
      return SamRetry.retry(usersApi::getUserStatusInfo);
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error getting user status info from Sam", apiException);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      throw SamExceptionFactory.create(
          "Request interrupted while getting user status info from Sam", interruptedException);
    }
  }

  @Bean("cromwellWriteClient")
  @RequestScope
  public cromwell.client.ApiClient cromwellWriteClient(
      BearerToken bearerToken, CromwellClient cromwellClient)
      throws DependencyNotAvailableException {

    return cromwellClient.getWriteApiClient(bearerToken.getToken());
  }
}
