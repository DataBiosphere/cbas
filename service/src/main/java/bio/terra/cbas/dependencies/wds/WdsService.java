package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.databiosphere.workspacedata.model.VersionResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class WdsService implements HealthCheck {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;

  private final RetryTemplate listenerResetRetryTemplate;

  private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WdsService.class);

  public WdsService(
      WdsClient wdsClient,
      WdsServerConfiguration wdsServerConfiguration,
      RetryTemplate listenerResetRetryTemplate) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.listenerResetRetryTemplate = listenerResetRetryTemplate;
  }

  public RecordResponse getRecord(String recordType, String recordId) throws WdsServiceException {
    return executionWithRetryTemplate(
        listenerResetRetryTemplate,
        () ->
            wdsClient
                .recordsApi()
                .getRecord(
                    wdsServerConfiguration.instanceId(),
                    wdsServerConfiguration.apiV(),
                    recordType,
                    recordId));
  }

  public RecordResponse updateRecord(RecordRequest request, String type, String id)
      throws WdsServiceException {
    return executionWithRetryTemplate(
        listenerResetRetryTemplate,
        () -> {
          wdsClient
              .recordsApi()
              .updateRecord(
                  request,
                  wdsServerConfiguration.instanceId(),
                  wdsServerConfiguration.apiV(),
                  type,
                  id);
          return null;
        });
  }

  @Override
  public Result checkHealth() {
    try {
      VersionResponse result =
          executionWithRetryTemplate(
              listenerResetRetryTemplate, () -> wdsClient.generalWdsInformationApi().versionGet());
      return new Result(true, "WDS version is %s".formatted(result.getBuild().getVersion()));
    } catch (WdsServiceException e) {
      logger.error("WDS health check failed", e);
      return new Result(false, e.getMessage());
    }
  }

  interface WdsAction<T> {
    T execute() throws ApiException, DependencyNotAvailableException, AzureAccessTokenException;
  }

  public static <T> T executionWithRetryTemplate(RetryTemplate retryTemplate, WdsAction<T> action)
      throws WdsServiceException {

    // Why all this song and dance to catch exceptions and map them to almost identical exceptions?
    // Because the RetryTemplate's execute function only allows us to declare one Throwable type.
    // So we have a top-level WdsServiceException that we can catch and handle, and then we have
    // subclasses of that exception representing the types of exception that can be thrown. This
    // way, we can keep well typed exceptions (no "catch (Exception e)") and still make use of the
    // retry framework.
    return retryTemplate.execute(
        context -> {
          try {
            return action.execute();
          } catch (ApiException e) {
            throw new WdsServiceApiException(e);
          } catch (DependencyNotAvailableException e) {
            throw new WdsServiceNotAvailableException(e);
          } catch (AzureAccessTokenException e) {
            throw new WdsTokenNotAvailableException(e);
          }
        });
  }
}
