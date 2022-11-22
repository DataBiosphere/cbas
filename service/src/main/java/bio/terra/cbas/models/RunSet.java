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

  // Corresponding table column names in database
  public static final String ID = "id";
  public static final String STATUS = "status";
  public static final String METHOD_ID = "method_id";
  public static final String SUBMISSION_TIMESTAMP = "submission_timestamp";
  public static final String LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
  public static final String LAST_POLLED_TIMESTAMP = "last_polled_timestamp";
  public static final String RUN_COUNT = "run_count";
  public static final String ERROR_COUNT = "error_count";

  public UUID getMethodId() {
    return method.id();
  }
}
