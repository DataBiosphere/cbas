package bio.terra.cbas.controllers;

import bio.terra.cbas.config.BeanConfig;
import bio.terra.cbas.model.ErrorReport;
import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import bio.terra.common.exception.ErrorReportException;
import bio.terra.common.iam.BearerToken;
import java.util.List;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <a
 * href="https://github.com/DataBiosphere/terra-java-project-template/blob/43cff86354e8d295ed96c29a7faf60eadd11e502/service/src/main/java/bio/terra/javatemplate/controller/GlobalExceptionHandler.java">Taken
 * from Terra template</a> This enables proper logging and response codes when errors are thrown
 * within the code (primarily when interacting with Sam). Without this, any errors are reported to
 * the user as a 500 Internal Server Error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler<ErrorReport> {
  /**
   * This method allows for error uncovering of custom exceptions that we throw inside bean
   * creation, especially the {@link BearerToken} bean in {@link BeanConfig}. If the root cause does
   * not stem from our own custom errors, it will be reported by our catch all handler as an
   * internal server error.
   */
  @ExceptionHandler(BeanCreationException.class)
  public ResponseEntity<ErrorReport> beanCreationErrorHandler(BeanCreationException ex) {
    if (ex.getRootCause() instanceof ErrorReportException errorReportException) {
      return this.errorReportHandler(errorReportException);
    } else {
      return this.catchallHandler(ex);
    }
  }

  @Override
  public ErrorReport generateErrorReport(Throwable ex, HttpStatus statusCode, List<String> causes) {
    return new ErrorReport().message(ex.getMessage()).statusCode(statusCode.value());
  }
}
