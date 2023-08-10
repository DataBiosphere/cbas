package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.exception.ErrorReportException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SamService implements HealthCheck {

  private final SamClient samClient;
  private final BearerToken bearerToken;

  // Sam resource type name for Workspaces
  public static final String RESOURCE_TYPE_WORKSPACE = "workspace";

  // Sam action name for read permission
  public static final String READ_ACTION = "read";

  // Sam action name for write permission
  public static final String WRITE_ACTION = "write";

  // Sam action name for compute  permission
  public static final String COMPUTE_ACTION = "compute";

  private static final Logger logger = LoggerFactory.getLogger(SamService.class);

  public SamService(SamClient samClient, BearerToken bearerToken) {
    this.samClient = samClient;
    this.bearerToken = bearerToken;
  }

  private StatusApi getStatusApi() {
    return new StatusApi(samClient.getApiClient());
  }

  boolean hasPermission(String actionType) {
    // don't check auth access with Sam if "sam.checkAuthAccess" is false
    if (!samClient.checkAuthAccessWithSam()) return true;

    logger.debug(
        "Checking Sam permission for '{}' resource and '{}' action type for user on workspace '{}'.",
        RESOURCE_TYPE_WORKSPACE,
        actionType,
        samClient.getWorkspaceId());

    try {
      ResourcesApi resourcesApi = getResourcesApi();
      return SamRetry.retry(
          () ->
              resourcesApi.resourcePermissionV2(
                  RESOURCE_TYPE_WORKSPACE, samClient.getWorkspaceId(), actionType));
    } catch (ApiException e) {
      throw SamExceptionFactory.create(
          "Error checking %s permissions on workspace from Sam".formatted(actionType), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw SamExceptionFactory.create(
          "Request interrupted while checking %s permissions on workspace from Sam"
              .formatted(actionType),
          e);
    }
  }

  public UsersApi getUsersApi() {
    return new UsersApi(samClient.getApiClient(bearerToken.getToken()));
  }

  public ResourcesApi getResourcesApi() {
    return new ResourcesApi(samClient.getApiClient(bearerToken.getToken()));
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

  public boolean hasReadPermission() {
    return hasPermission(READ_ACTION);
  }

  public boolean hasWritePermission() {
    return hasPermission(WRITE_ACTION);
  }

  public boolean hasComputePermission() {
    return hasPermission(COMPUTE_ACTION);
  }
}
