package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Method(
    UUID method_id,
    String name,
    String description,
    OffsetDateTime created,
    OffsetDateTime lastRun,
    String methodSource,
    String methodSourceUrl) {

  // Corresponding table column names in database
  public static final String METHOD_ID_COL = "method_id";
  public static final String NAME_COL = "name";
  public static final String DESCRIPTION__COL = "description";
  public static final String CREATED_COL = "created";
  public static final String LAST_RUN_COL = "last_run";
  public static final String METHOD_SOURCE_COL = "method_source";
  public static final String METHOD_SOURCE_URL_COL = "method_source_url";
}
