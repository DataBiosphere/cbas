package bio.terra.cbas.models;

import java.util.UUID;

public record RunSet(UUID id, Method method) {

  public UUID getMethodId() {
    return method.id();
  }
}
