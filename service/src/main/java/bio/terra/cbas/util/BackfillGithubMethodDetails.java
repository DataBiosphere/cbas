package bio.terra.cbas.util;

import static bio.terra.cbas.common.MethodUtil.convertGithubToRawUrl;
import static bio.terra.cbas.common.MethodUtil.extractGithubDetailsFromUrl;

import bio.terra.cbas.util.methods.GithubUrlComponents;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import liquibase.Scope;
import liquibase.change.custom.CustomSqlChange;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;

public class BackfillGithubMethodDetails implements CustomTaskChange, CustomSqlChange {

  /*
     Backfill details to github_details tables:
       - ✅find the methods in `method` table that have method_source=GITHUB and doesn't have corresponding rows in github_details table
       - ✅for each of these method_id, get the method_version_url from `method_version` table (also extract the method_version_id)
       - ✅use the `extractGithubMethodDetails` utility function to fetch the repo, organization, path (private is set to false)
       - ✅add these details to respective tables
       - update method_version table with branch_ot_tag_name information
  */

  @Override
  public void execute(Database database) throws CustomChangeException {
    Scope.getCurrentScope().getLog(getClass()).info("Hello World!");

    try {
      JdbcConnection connection = (JdbcConnection) database.getConnection();

      // find the methods in `method` table that have method_source=GITHUB and don't have and
      // corresponding rows in github_details table

      var methodIdStmt = connection.createStatement();
      ResultSet methodIdResultSet =
          methodIdStmt.executeQuery(
              "select method_id from method "
                  + "where method_source='GitHub'"
                  + "and method_id not in ("
                  + "select method_id from github_method_details"
                  + ");");

      while (methodIdResultSet.next()) {

        // for each method, get the method_version_url from `method_version` table

        Scope.getCurrentScope()
            .getLog(getClass())
            .info("Found methodIdResultSet - %s".formatted(methodIdResultSet.getString(1)));

        String methodUrlQuery = "select method_version_url from method_version where method_id = ?";
        PreparedStatement methodUrlStmt = connection.prepareStatement(methodUrlQuery);
        methodUrlStmt.setObject(1, UUID.fromString(methodIdResultSet.getString(1)));

        ResultSet methodUrlResultSet = methodUrlStmt.executeQuery();

        if (methodUrlResultSet.next()) {

          // use the `extractGithubMethodDetails` utility function to fetch the repo, organization,
          // path (private is set to false)
          // add these details to respective tables

          Scope.getCurrentScope()
              .getLog(getClass())
              .info(
                  "method_version_url for method_id %s - %s"
                      .formatted(methodIdResultSet.getString(1), methodUrlResultSet.getString(1)));

          String rawUrl = convertGithubToRawUrl(methodUrlResultSet.getString(1));
          GithubUrlComponents githubMethodDetails = extractGithubDetailsFromUrl(rawUrl);

          Scope.getCurrentScope()
              .getLog(getClass())
              .info(
                  "Method details: org - %s\t repo - %s\t path - %s\t branch - %s"
                      .formatted(
                          githubMethodDetails.org(),
                          githubMethodDetails.repo(),
                          githubMethodDetails.path(),
                          githubMethodDetails.branchOrTag()));

          String insertQuery =
              "insert into github_method_details(repository, organization, path, private, method_id) values (?, ?, ?, false, ?)";
          PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
          insertStmt.setString(1, githubMethodDetails.repo());
          insertStmt.setString(2, githubMethodDetails.org());
          insertStmt.setString(3, githubMethodDetails.path());
          insertStmt.setObject(4, UUID.fromString(methodIdResultSet.getString(1)));

          int insertResultSet = insertStmt.executeUpdate();

          if (insertResultSet == 1) {
            Scope.getCurrentScope().getLog(getClass()).info("INSERT stmt execution returned 1");
          }
        } else {
          // TODO: it shouldn't happen that a method_version_url is null - what to do here?
          Scope.getCurrentScope()
              .getLog(getClass())
              .info(
                  "NO method_version_url found for method_id %s"
                      .formatted(methodIdResultSet.getString(1)));
        }

        methodUrlStmt.close();
      }

    } catch (Exception e) {
      throw new CustomChangeException(e);
    }
  }

  @Override
  public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
    return new SqlStatement[0];
  }

  @Override
  public String getConfirmationMessage() {
    return "Called getConfirmationMessage()";
  }

  @Override
  public void setUp() throws SetupException {}

  @Override
  public ValidationErrors validate(Database database) {
    Scope.getCurrentScope().getLog(getClass()).info("called validate()");
    return null;
  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {
    // do nothing since we don't need additional resources
  }
}
