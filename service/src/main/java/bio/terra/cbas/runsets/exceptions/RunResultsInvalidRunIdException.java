package bio.terra.cbas.runsets.exceptions;

import bio.terra.common.exception.BadRequestException;

public class RunResultsInvalidRunIdException extends BadRequestException {
  public RunResultsInvalidRunIdException(String message) {
    super(message);
  }
}
