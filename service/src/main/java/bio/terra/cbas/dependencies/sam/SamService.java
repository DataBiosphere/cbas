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
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class SamService implements HealthCheck {

  private final SamClient samClient;

  // Sam resource type name for Workspaces
  public static final String RESOURCE_TYPE_WORKSPACE = "workspace";

  // Sam action name for read permission
  public static final String READ_ACTION = "read";

  // Sam action name for write permission
  public static final String WRITE_ACTION = "write";

  private static final Logger logger = LoggerFactory.getLogger(SamService.class);

  public SamService(SamClient samClient) {
    this.samClient = samClient;
  }

  private StatusApi getStatusApi() {
    return new StatusApi(samClient.getApiClient());
  }

  public boolean hasPermission(String actionType, UserStatusInfo userInfo, BearerToken userToken) {
    // don't check auth access with Sam if "sam.checkAuthAccess" is false
    if (!samClient.checkAuthAccessWithSam()) return true;

    /* Associate user id with log context for this thread/request. This will be included with
    request ID within any logs for the remainder of the request. See
    https://github.com/DataBiosphere/terra-common-lib/blob/eaaf6217ec0f024afa45aac14d21c8964c0f27c5/src/main/java/bio/terra/common/logging/GoogleJsonLayout.java#L129-L132
    for details on how this is included in cloud logs, and logback.xml for console logs
    */
    MDC.put("user", userInfo.getUserSubjectId());

    logger.debug(
        "Checking Sam permission for '{}' resource and '{}' action type for user on workspace '{}'.",
        RESOURCE_TYPE_WORKSPACE,
        actionType,
        samClient.getWorkspaceId());

    try {
      ResourcesApi resourcesApi = getResourcesApi(userToken);
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

  public UsersApi getUsersApi(BearerToken userToken) {
    return new UsersApi(samClient.getApiClient(userToken));
  }

  public ResourcesApi getResourcesApi(BearerToken userToken) {
    return new ResourcesApi(samClient.getApiClient(userToken));
  }

  // Borrowed from WDS
  public UserStatusInfo getSamUser(BearerToken userToken) throws ErrorReportException {
    if (!samClient.checkAuthAccessWithSam()) {
      return new UserStatusInfo(); // Dummy user for local testing
    }
    UsersApi usersApi = getUsersApi(userToken);
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

  public boolean hasReadPermission(BearerToken userToken) {
    return hasPermission(READ_ACTION, getSamUser(userToken), userToken);
  }

  public boolean hasWritePermission(BearerToken userToken) {
    return hasPermission(WRITE_ACTION, getSamUser(userToken), userToken);
  }
}
