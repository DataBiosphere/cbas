package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public record Method(
    UUID methodId,
    String name,
    String description,
    OffsetDateTime created,
    UUID lastRunSetId,
    String methodSource,
    UUID originalWorkspaceId,
    Optional<GithubMethodDetails> githubMethodDetails,
    CbasMethodStatus methodStatus) {

  // Corresponding table column names in database
  public static final String METHOD_ID_COL = "method_id";
  public static final String NAME_COL = "name";
  public static final String DESCRIPTION__COL = "description";
  public static final String CREATED_COL = "created";
  public static final String LAST_RUN_SET_ID_COL = "last_run_set_id";
  public static final String METHOD_SOURCE_COL = "method_source";
  public static final String ORIGINAL_WORKSPACE_ID_COL = "method_original_workspace_id";
  public static final String METHOD_STATUS_COL = "method_status";

  public CbasMethodStatus getMethodStatus() {
    return methodStatus;
  }

  public Method withGithubMethodDetails(GithubMethodDetails githubMethodDetails) {
    return new Method(
        methodId,
        name,
        description,
        created,
        lastRunSetId,
        methodSource,
        originalWorkspaceId,
        Optional.ofNullable(githubMethodDetails),
        CbasMethodStatus.ACTIVE);
  }
}
