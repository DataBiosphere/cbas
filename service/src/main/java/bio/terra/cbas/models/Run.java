package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Run(
    UUID runId,
    String engineId,
    RunSet runSet,
    String recordId,
    OffsetDateTime submissionTimestamp,
    CbasRunStatus status,
    OffsetDateTime lastModifiedTimestamp,
    OffsetDateTime lastPolledTimestamp,
    String errorMessages,
    String originalWorkspaceId) {

  // Corresponding table column names in database
  public static final String RUN_ID_COL = "run_id";
  public static final String RUN_SET_ID_COL = "run_set_id";
  public static final String ENGINE_ID_COL = "engine_id";
  public static final String RECORD_ID_COL = "record_id";
  public static final String SUBMISSION_TIMESTAMP_COL = "submission_timestamp";
  public static final String STATUS_COL = "status";
  public static final String LAST_MODIFIED_TIMESTAMP_COL = "last_modified_timestamp";
  public static final String LAST_POLLED_TIMESTAMP_COL = "last_polled_timestamp";
  public static final String ERROR_MESSAGES_COL = "error_messages";
  public static final String ORIGINAL_WORKSPACE_ID_COL = "run_original_workspace_id";

  public UUID getRunSetId() {
    return runSet.runSetId();
  }

  public Run copy() {
    return new Run(
        runId,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        status,
        lastModifiedTimestamp,
        lastPolledTimestamp,
        errorMessages,
        originalWorkspaceId);
  }

  public Run withStatus(CbasRunStatus newStatus) {
    return new Run(
        runId,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        newStatus,
        lastModifiedTimestamp,
        lastPolledTimestamp,
        errorMessages,
        originalWorkspaceId);
  }

  public Run withErrorMessage(String message) {
    return new Run(
        runId,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        status,
        lastModifiedTimestamp,
        lastPolledTimestamp,
        message,
        originalWorkspaceId);
  }

  public Run withLastModified(OffsetDateTime newLastModifiedTimestamp) {
    return new Run(
        runId,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        status,
        newLastModifiedTimestamp,
        lastPolledTimestamp,
        errorMessages,
        originalWorkspaceId);
  }

  public Run withLastPolled(OffsetDateTime newLastPolledTimestamp) {
    return new Run(
        runId,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        status,
        lastModifiedTimestamp,
        newLastPolledTimestamp,
        errorMessages,
        originalWorkspaceId);
  }

  public Run withErrorMessages(String newErrorMessages) {
    return new Run(
        runId,
        engineId,
        runSet,
        recordId,
        submissionTimestamp,
        status,
        lastModifiedTimestamp,
        lastPolledTimestamp,
        newErrorMessages,
        originalWorkspaceId);
  }
}
