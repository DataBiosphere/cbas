package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RunSet(
    UUID runSetId,
    MethodVersion methodVersion,
    String name,
    String description,
    Boolean callCachingEnabled,
    Boolean isTemplate,
    CbasRunSetStatus status,
    OffsetDateTime submissionTimestamp,
    OffsetDateTime lastModifiedTimestamp,
    OffsetDateTime lastPolledTimestamp,
    Integer runCount,
    Integer errorCount,
    String inputDefinition,
    String outputDefinition,
    String recordType,
    String userId,
    UUID originalWorkspaceId) {

  // Corresponding table column names in database
  public static final String RUN_SET_ID_COL = "run_set_id";
  public static final String STATUS_COL = "status";
  public static final String NAME_COL = "run_set_name";
  public static final String DESCRIPTION_COL = "run_set_description";
  public static final String CALL_CACHING_ENABLED_COL = "call_caching_enabled";
  public static final String IS_TEMPLATE_COL = "is_template";
  public static final String SUBMISSION_TIMESTAMP_COL = "submission_timestamp";
  public static final String LAST_MODIFIED_TIMESTAMP_COL = "last_modified_timestamp";
  public static final String LAST_POLLED_TIMESTAMP_COL = "last_polled_timestamp";
  public static final String RUN_COUNT_COL = "run_count";
  public static final String ERROR_COUNT_COL = "error_count";
  public static final String INPUT_DEFINITION_COL = "input_definition";
  public static final String OUTPUT_DEFINITION_COL = "output_definition";
  public static final String RECORD_TYPE_COL = "record_type";
  public static final String USER_ID_COL = "user_id";
  public static final String ORIGINAL_WORKSPACE_ID_COL = "run_set_original_workspace_id";

  public UUID getMethodVersionId() {
    return methodVersion.methodVersionId();
  }
}
