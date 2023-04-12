package bio.terra.cbas.dependencies.common;

public interface HealthCheckable {

  public record HealthCheckResult(boolean isOk, String message) {}

  HealthCheckResult checkHealth();
}
