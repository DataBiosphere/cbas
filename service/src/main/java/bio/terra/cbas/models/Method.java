package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Method(
    UUID methodId,
    String name,
    String description,
    OffsetDateTime created,
    UUID lastRunSetId,
    String methodSource,
    UUID originalWorkspaceId) {

  // Corresponding table column names in database
  public static final String METHOD_ID_COL = "method_id";
  public static final String NAME_COL = "name";
  public static final String DESCRIPTION__COL = "description";
  public static final String CREATED_COL = "created";
  public static final String LAST_RUN_SET_ID_COL = "last_run_set_id";
  public static final String METHOD_SOURCE_COL = "method_source";
  public static final String ORIGINAL_WORKSPACE_ID_COL = "method_original_workspace_id";
}
