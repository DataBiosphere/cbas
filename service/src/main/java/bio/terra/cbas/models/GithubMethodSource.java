package bio.terra.cbas.models;

import java.util.UUID;

public record GithubMethodSource(
    String repository, String organization, String path, Boolean _private, UUID methodId) {

  // Corresponding table column names in database
  public static final String REPOSITORY_COL = "repository";
  public static final String ORGANIZATION_COL = "organization";
  public static final String PATH_COL = "path";
  public static final String PRIVATE_COL = "private";
  public static final String METHOD_ID_COL = "method_id";
}
