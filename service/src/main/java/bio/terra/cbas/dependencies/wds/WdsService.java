package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.databiosphere.workspacedata.model.VersionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class WdsService implements HealthCheck {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;

  @Autowired private RetryTemplate listenerResetRetryTemplate;

  private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WdsService.class);

  public WdsService(WdsClient wdsClient, WdsServerConfiguration wdsServerConfiguration) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
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
    } catch (Exception e) {
      logger.error("WDS health check failed", e);
      return new Result(false, e.getMessage());
    }
  }

  interface WdsAction<T> {
    T execute() throws ApiException, DependencyNotAvailableException, AzureAccessTokenException;
  }

  public static <T> T executionWithRetryTemplate(RetryTemplate retryTemplate, WdsAction<T> action)
      throws WdsServiceException {
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
