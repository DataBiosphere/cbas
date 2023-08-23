package bio.terra.cbas.runsets.exceptions;

import bio.terra.common.exception.BadRequestException;

public class RunResultsInvalidStatusException extends BadRequestException {
  public RunResultsInvalidStatusException(String message) {
    super(message);
  }
}
