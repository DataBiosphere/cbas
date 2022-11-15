package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Run(
    UUID id,
    String engineId,
    RunSet runSet,
    String recordId,
    OffsetDateTime submissionTimestamp,
    CbasRunStatus status,
    OffsetDateTime lastModifiedTimestamp,
    OffsetDateTime lastPolledTimestamp,
    String errorMessages) {

  public UUID getRunSetId() {
    return runSet.id();
  }

  public Run withStatus(CbasRunStatus newStatus) {
    return new Run(
        id,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        newStatus,
        lastModifiedTimestamp,
        lastPolledTimestamp,
        errorMessages);
  }

  public Run withErrorMessage(String message) {
    return new Run(
        id,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        status,
        lastModifiedTimestamp,
        lastPolledTimestamp,
        message);
  }
}
