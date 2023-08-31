package bio.terra.cbas.common.exceptions;

import bio.terra.common.exception.ErrorReportException;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;

public class RunNotFoundException extends ErrorReportException {

  public RunNotFoundException(String message) {
    super(message, new ArrayList<>(), HttpStatus.BAD_REQUEST);
  }
}
