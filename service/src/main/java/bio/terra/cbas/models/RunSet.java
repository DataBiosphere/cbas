package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RunSet(
    UUID runSetId,
    Method method,
    CbasRunSetStatus status,
    OffsetDateTime submissionTimestamp,
    OffsetDateTime lastModifiedTimestamp,
    OffsetDateTime lastPolledTimestamp,
    Integer runCount,
    Integer errorCount,
    String inputDefinition,
    String outputDefinition,
    String recordType) {

  // Corresponding table column names in database
  public static final String RUN_SET_ID_COL = "runSetId";
  public static final String STATUS_COL = "status";
  public static final String METHOD_ID_COL = "method_id";
  public static final String SUBMISSION_TIMESTAMP_COL = "submission_timestamp";
  public static final String LAST_MODIFIED_TIMESTAMP_COL = "last_modified_timestamp";
  public static final String LAST_POLLED_TIMESTAMP_COL = "last_polled_timestamp";
  public static final String RUN_COUNT_COL = "run_count";
  public static final String ERROR_COUNT_COL = "error_count";
  public static final String INPUT_DEFINITION_COL = "input_definition";
  public static final String OUTPUT_DEFINITION_COL = "output_definition";
  public static final String RECORD_TYPE_COL = "record_type";

  public UUID getMethodId() {
    return method.method_id();
  }
}
