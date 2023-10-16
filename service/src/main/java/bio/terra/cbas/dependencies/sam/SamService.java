package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class SamService implements HealthCheck {

  private final SamClient samClient;
  private final BearerToken bearerToken;
  private final UserStatusInfo userInfo;

  // Sam resource type name for Workspaces
  public static final String RESOURCE_TYPE_WORKSPACE = "workspace";

  // Sam action name for read permission
  public static final String READ_ACTION = "read";

  // Sam action name for write permission
  public static final String WRITE_ACTION = "write";

  // Sam action name for compute  permission
  public static final String COMPUTE_ACTION = "compute";

  private static final Logger logger = LoggerFactory.getLogger(SamService.class);

  public SamService(SamClient samClient, BearerToken bearerToken, UserStatusInfo userInfo) {
    this.samClient = samClient;
    this.bearerToken = bearerToken;
    this.userInfo = userInfo;
  }

  private StatusApi getStatusApi() {
    return new StatusApi(samClient.getApiClient());
  }

  boolean hasPermission(String actionType) {
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

  public ResourcesApi getResourcesApi() {
    return new ResourcesApi(samClient.getApiClient(bearerToken.getToken()));
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
