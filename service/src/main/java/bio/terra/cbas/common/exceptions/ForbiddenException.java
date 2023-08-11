package bio.terra.cbas.common.exceptions;

import bio.terra.common.exception.ErrorReportException;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends ErrorReportException {

  public ForbiddenException(String errorMsg) {
    super(errorMsg, new ArrayList<>(), HttpStatus.FORBIDDEN);
  }
}
