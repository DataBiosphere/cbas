package bio.terra.cbas.runsets.exceptions;

import bio.terra.common.exception.InternalServerErrorException;

public class RunResultsUpdateErrorException extends InternalServerErrorException {
  public RunResultsUpdateErrorException(String message) {
    super(message);
  }
}
