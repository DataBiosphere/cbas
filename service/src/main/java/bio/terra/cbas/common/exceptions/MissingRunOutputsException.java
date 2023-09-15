package bio.terra.cbas.common.exceptions;

import bio.terra.common.exception.ErrorReportException;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;

public class MissingRunOutputsException extends ErrorReportException {

  public MissingRunOutputsException(String message) {
    super(message, new ArrayList<>(), HttpStatus.BAD_REQUEST);
  }
}
