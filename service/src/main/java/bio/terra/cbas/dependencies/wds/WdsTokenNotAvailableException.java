package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;

public class WdsTokenNotAvailableException extends WdsServiceException {
  private final AzureAccessTokenException exception;

  public WdsTokenNotAvailableException(AzureAccessTokenException exception) {
    this.exception = exception;
  }

  @Override
  public synchronized AzureAccessTokenException getCause() {
    return exception;
  }
}
