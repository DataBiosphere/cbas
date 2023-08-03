package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.exception.ErrorReportException;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class SamService implements HealthCheck {

  private final SamClient samClient;

  public SamService(SamClient samClient) {
    this.samClient = samClient;
  }

  private StatusApi getStatusApi() {
    return new StatusApi(samClient.getApiClient());
  }

  public UsersApi getUsersApi(String accessToken) {
    return new UsersApi(samClient.getApiClient(accessToken));
  }

  public String getUserToken() {
    // RequestContextHolder exposes the web request in the form of a *thread-bound*
    // [RequestAttributes](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/context/request/RequestAttributes.html) object.
    return (String)
        RequestContextHolder.currentRequestAttributes()
            .getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN, RequestAttributes.SCOPE_REQUEST);
  }

  // this cache uses token.hashCode as its key. This prevents any logging such as
  // in CacheLogger from logging the raw token.
  // Taken from WDS
  @Cacheable(cacheNames = "tokenResolution", key = "#root.target.getUserToken().hashCode()")
  public UserStatusInfo getSamUser() throws ErrorReportException {
    UsersApi usersApi = getUsersApi(getUserToken());
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

  @Override
  public Result checkHealth() {
    try {
      SystemStatus result = getStatusApi().getSystemStatus();
      return new Result(result.getOk(), result.toString());
    } catch (ApiException e) {
      return new Result(false, e.getMessage());
    }
  }
}
