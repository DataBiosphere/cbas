package bio.terra.cbas.dependencies.wds;

import org.databiosphere.workspacedata.client.ApiException;

public class WdsServiceApiException extends WdsServiceException {
  private final ApiException exception;

  public WdsServiceApiException(ApiException exception) {
    this.exception = exception;
  }

  @Override
  public ApiException getCause() {
    return exception;
  }
}
