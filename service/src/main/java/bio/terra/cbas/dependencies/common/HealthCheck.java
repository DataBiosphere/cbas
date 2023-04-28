package bio.terra.cbas.dependencies.common;

public interface HealthCheck {

  record Result(boolean isOk, String message) {}

  Result checkHealth();
}
