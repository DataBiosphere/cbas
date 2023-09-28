package bio.terra.cbas.common.exceptions;

import bio.terra.common.exception.ErrorReportException;

public class DependencyNotAvailableException extends ErrorReportException {
  public DependencyNotAvailableException(String dependency, String context) {
    super("Dependency not available: %s. %s".formatted(dependency, context));
  }

  public DependencyNotAvailableException(String dependency, String context, Throwable reason) {
    super(
        "Dependency not available: %s. %s%n%s".formatted(dependency, context, reason.getMessage()),
        reason);
  }
}
