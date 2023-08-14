package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.exception.ErrorReportException;
import bio.terra.common.iam.BearerToken;
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
  private final BearerToken bearerToken;

  public SamService(SamClient samClient, BearerToken bearerToken) {
    this.samClient = samClient;
    this.bearerToken = bearerToken;
  }

  private StatusApi getStatusApi() {
    return new StatusApi(samClient.getApiClient());
  }

  public UsersApi getUsersApi() {
    return new UsersApi(samClient.getApiClient(bearerToken.getToken()));
  }

  // Borrowed from WDS
  public UserStatusInfo getSamUser() throws ErrorReportException {
    UsersApi usersApi = getUsersApi();
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
