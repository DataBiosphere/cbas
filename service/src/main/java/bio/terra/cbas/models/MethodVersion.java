package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public record MethodVersion(
    UUID methodVersionId,
    Method method,
    String name,
    String description,
    OffsetDateTime created,
    UUID lastRunSetId,
    String url,
    UUID originalWorkspaceId,
    String branchOrTagName,
    Optional<GithubMethodVersionDetails> methodVersionDetails) {

  // Corresponding table column names in database
  public static final String METHOD_VERSION_ID_COL = "method_version_id";
  public static final String METHOD_ID_COL = "method_id";
  public static final String NAME_COL = "method_version_name";
  public static final String DESCRIPTION__COL = "method_version_description";
  public static final String CREATED_COL = "method_version_created";
  public static final String LAST_RUN_SET_ID_COL = "method_version_last_run_set_id";
  public static final String URL_COL = "method_version_url";
  public static final String ORIGINAL_WORKSPACE_ID_COL = "method_version_original_workspace_id";
  public static final String BRANCH_OR_TAG_NAME = "branch_or_tag_name";

  public UUID getMethodId() {
    return method.methodId();
  }

  public UUID getOriginalWorkspaceId() {
    return originalWorkspaceId;
  }

  public MethodVersion withMethodVersionId(UUID methodVersionId) {
    return new MethodVersion(
        methodVersionId,
        method,
        name,
        description,
        created,
        lastRunSetId,
        url,
        originalWorkspaceId,
        branchOrTagName,
        methodVersionDetails);
  }

  public MethodVersion withMethodVersionDetails(GithubMethodVersionDetails methodVersionDetails) {
    return new MethodVersion(
        methodVersionId,
        method,
        name,
        description,
        created,
        lastRunSetId,
        url,
        originalWorkspaceId,
        branchOrTagName,
        Optional.of(methodVersionDetails));
  }
}
