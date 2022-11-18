package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RunSet(
    UUID id,
    Method method,
    CbasRunSetStatus status,
    OffsetDateTime submissionTimestamp,
    OffsetDateTime lastModifiedTimestamp,
    OffsetDateTime lastPolledTimestamp,
    Integer runCount,
    Integer errorCount) {

  public UUID getMethodId() {
    return method.id();
  }
}
