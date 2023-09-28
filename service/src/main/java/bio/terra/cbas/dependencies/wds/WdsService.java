package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.common.iam.BearerToken;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class WdsService {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;
  private final RetryTemplate listenerResetRetryTemplate;

  private final BearerToken bearerToken;

  public WdsService(
      WdsClient wdsClient,
      WdsServerConfiguration wdsServerConfiguration,
      RetryTemplate listenerResetRetryTemplate,
      BearerToken bearerToken) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.listenerResetRetryTemplate = listenerResetRetryTemplate;
    this.bearerToken = bearerToken;
  }

  public RecordResponse getRecord(String recordType, String recordId) throws WdsServiceException {
    return executionWithRetryTemplate(
        listenerResetRetryTemplate,
        () ->
            wdsClient
                .recordsApi(bearerToken.getToken())
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
              .recordsApi(bearerToken.getToken())
              .updateRecord(
                  request,
                  wdsServerConfiguration.instanceId(),
                  wdsServerConfiguration.apiV(),
                  type,
                  id);
          return null;
        });
  }

  interface WdsAction<T> {
    T execute() throws ApiException, DependencyNotAvailableException;
  }

  @SuppressWarnings("java:S125") // The comment here isn't "commented code"
  static <T> T executionWithRetryTemplate(RetryTemplate retryTemplate, WdsAction<T> action)
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
          }
        });
  }
}
