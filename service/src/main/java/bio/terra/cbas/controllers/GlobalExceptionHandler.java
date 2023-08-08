package bio.terra.cbas.controllers;

import bio.terra.cbas.model.ErrorReport;
import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import java.util.List;
import org.springframework.http.HttpStatus;
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

  @Override
  public ErrorReport generateErrorReport(Throwable ex, HttpStatus statusCode, List<String> causes) {
    return new ErrorReport().message(ex.getMessage()).statusCode(statusCode.value());
  }
}
