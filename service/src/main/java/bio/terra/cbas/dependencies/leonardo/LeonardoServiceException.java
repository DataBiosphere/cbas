package bio.terra.cbas.dependencies.leonardo;

import bio.terra.common.exception.ErrorReportException;
import java.util.List;
import org.springframework.http.HttpStatus;

public abstract class LeonardoServiceException extends ErrorReportException {
  protected LeonardoServiceException(String message, Throwable cause) {
    super(message, cause, List.of(cause.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
