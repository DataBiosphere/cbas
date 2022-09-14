package bio.terra.cbas.exception;

import bio.terra.common.exception.NotFoundException;

public class RunQueryNotFoundException extends NotFoundException {

  public RunQueryNotFoundException(String message) {
    super(message);
  }

  public RunQueryNotFoundException(String message, Throwable reason) {
    super(message, reason);
  }
}
