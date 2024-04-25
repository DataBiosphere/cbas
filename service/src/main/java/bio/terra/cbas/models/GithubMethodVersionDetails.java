package bio.terra.cbas.models;

import java.util.UUID;

public record GithubMethodVersionDetails(String githash, UUID methodVersionId) {
  public static final String GITHASH_COL = "githash";
  public static final String METHOD_VERSION_ID_COL = "method_version_id";
}
