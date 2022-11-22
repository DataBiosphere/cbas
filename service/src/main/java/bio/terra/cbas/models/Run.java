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

  // Corresponding table column names in database
  public static final String ID = "id";
  public static final String RUN_SET_ID = "run_set_id";
  public static final String ENGINE_ID = "engine_id";
  public static final String RECORD_ID = "record_id";
  public static final String SUBMISSION_TIMESTAMP = "submission_timestamp";
  public static final String STATUS = "status";
  public static final String LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
  public static final String LAST_POLLED_TIMESTAMP = "last_polled_timestamp";
  public static final String ERROR_MESSAGES = "error_messages";

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
