package bio.terra.cbas.dependencies.wds;

public abstract class WdsServiceException extends Exception {
  @Override
  public String getMessage() {
    return "%s: %s".formatted(getClass().getSimpleName(), getCause().getMessage());
  }
}
