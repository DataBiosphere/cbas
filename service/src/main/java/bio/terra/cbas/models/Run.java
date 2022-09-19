package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Run(
    UUID id,
    String engineId,
    RunSet runSet,
    String entityId,
    OffsetDateTime submissionTimestamp,
    String status) {

  public UUID runSetId() {
    return runSet.id();
  }
}
