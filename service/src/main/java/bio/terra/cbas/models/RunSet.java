package bio.terra.cbas.models;

import java.util.UUID;

public record RunSet(UUID id, Method method) {

  public UUID methodId() {
    return method.id();
  }
}
