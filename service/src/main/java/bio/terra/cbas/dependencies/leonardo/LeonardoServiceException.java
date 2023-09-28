package bio.terra.cbas.dependencies.leonardo;

import bio.terra.common.exception.ErrorReportException;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;

public abstract class LeonardoServiceException extends ErrorReportException {
  protected LeonardoServiceException(String message) {
    super(message, new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
