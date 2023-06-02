package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;

public class WdsServiceNotAvailableException extends WdsServiceException {
  private final DependencyNotAvailableException exception;

  public WdsServiceNotAvailableException(DependencyNotAvailableException exception) {
    this.exception = exception;
  }

  @Override
  public synchronized DependencyNotAvailableException getCause() {
    return exception;
  }
}
