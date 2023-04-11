package bio.terra.cbas.common.exceptions;

public class DependencyNotAvailableException extends Exception {
  public DependencyNotAvailableException(String dependency, String context) {
    super("Dependency not available: %s. %s".formatted(dependency, context));
  }

  public DependencyNotAvailableException(String dependency, String context, Throwable reason) {
    super("Dependency not available: %s. %s".formatted(dependency, context), reason);
  }
}
