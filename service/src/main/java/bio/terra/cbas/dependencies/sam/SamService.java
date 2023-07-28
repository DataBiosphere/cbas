package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.springframework.stereotype.Component;

@Component
public class SamService implements HealthCheck {

  private final SamClient samClient;

  public SamService(SamClient samClient) {
    this.samClient = samClient;
  }

  private StatusApi getStatusApi() {
    return new StatusApi(samClient.getApiClient());
  }

  private UsersApi samUsersApi(String accessToken) {
    return new UsersApi(samClient.getApiClient(accessToken));
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
    UsersApi usersApi = samUsersApi(accessToken);
    try {
      return SamRetry.retry(usersApi::getUserStatusInfo);
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error getting user status info from Sam", apiException);
    } catch (InterruptedException interruptedException) {
      throw SamExceptionFactory.create(
          "Request interrupted while getting user status info from Sam", interruptedException);
    }
  }
}
