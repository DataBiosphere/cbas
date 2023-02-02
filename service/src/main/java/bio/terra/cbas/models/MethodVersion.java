package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MethodVersion(
    UUID methodVersionId,
    Method method,
    String name,
    String description,
    OffsetDateTime created,
    UUID lastRunSetId,
    String url) {

  // Corresponding table column names in database
  public static final String METHOD_VERSION_ID_COL = "method_version_id";
  public static final String NAME_COL = "method_version_name";
  public static final String DESCRIPTION__COL = "method_version_description";
  public static final String CREATED_COL = "method_version_created";
  public static final String LAST_RUN_SET_ID_COL = "method_version_last_run_set_id";
  public static final String URL_COL = "method_version_url";

  public UUID getMethodId() {
    return method.method_id();
  }
}
