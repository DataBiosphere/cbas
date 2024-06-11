package bio.terra.cbas.common.exceptions;

import java.util.UUID;

public class MethodNotFoundException extends RuntimeException {
  public MethodNotFoundException(UUID methodId) {
    super("Method %s not found".formatted(methodId));
  }
}
