package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
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

  UsersApi getUsersApi(String accessToken) {
    return new UsersApi(samClient.getApiClient(accessToken));
  }

  public Optional<String> getUserToken() {
    // RequestContextHolder exposes the web request in the form of a *thread-bound*
    // [RequestAttributes](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/context/request/RequestAttributes.html) object.
    Object token =
        RequestContextHolder.currentRequestAttributes()
            .getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN, RequestAttributes.SCOPE_REQUEST);
    return Optional.ofNullable((String) token);
  }

  public Optional<UserStatusInfo> getSamUser() {
    return getUserToken().map(this::getUserStatusInfo);
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

  public UserStatusInfo getUserStatusInfo(String accessToken) {
    UsersApi usersApi = getUsersApi(accessToken);
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
}
