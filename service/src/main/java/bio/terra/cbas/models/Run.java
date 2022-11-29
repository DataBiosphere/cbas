package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Run(
    UUID run_id,
    String engineId,
    RunSet runSet,
    String recordId,
    OffsetDateTime submissionTimestamp,
    CbasRunStatus status,
    OffsetDateTime lastModifiedTimestamp,
    OffsetDateTime lastPolledTimestamp,
    String errorMessages) {

  // Corresponding table column names in database
  public static final String RUN_ID_COL = "run_id";
  public static final String RUN_SET_ID_COL = "runSetId";
  public static final String ENGINE_ID_COL = "engine_id";
  public static final String RECORD_ID_COL = "record_id";
  public static final String SUBMISSION_TIMESTAMP_COL = "submission_timestamp";
  public static final String STATUS_COL = "status";
  public static final String LAST_MODIFIED_TIMESTAMP_COL = "last_modified_timestamp";
  public static final String LAST_POLLED_TIMESTAMP_COL = "last_polled_timestamp";
  public static final String ERROR_MESSAGES_COL = "error_messages";

  public UUID getRunSetId() {
    return runSet.runSetId();
  }

  public Run withStatus(CbasRunStatus newStatus) {
    return new Run(
        run_id,
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
        run_id,
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
