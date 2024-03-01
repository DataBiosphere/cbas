package bio.terra.cbas.util;

import static bio.terra.cbas.common.MethodUtil.convertGithubToRawUrl;
import static bio.terra.cbas.common.MethodUtil.extractGithubDetailsFromUrl;

import bio.terra.cbas.util.methods.GithubUrlComponents;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;
import liquibase.change.custom.CustomSqlChange;
import liquibase.changelog.column.LiquibaseColumn;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;

/**
 * Custom Java class called from the liquibase changeset
 * '20240223_backfill_github_method_details.yaml'. The purpose of this class is to backfill data for
 * GitHub methods into 'github_method_details' table.
 */
public class BackfillGithubMethodDetails implements CustomSqlChange {

  @Override
  public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
    try {
      JdbcConnection connection = (JdbcConnection) database.getConnection();
      ArrayList<SqlStatement> sqlStatementList = new ArrayList<>();
      SqlStatement[] sqlStatementArray = {};

      // find GitHub methods whose details aren't in 'github_method_details' table
      var methodIdStmt = connection.createStatement();
      ResultSet methodIdResultSet =
          methodIdStmt.executeQuery(
              "select method_id from method "
                  + "where method_source='GitHub'"
                  + "and method_id not in ("
                  + "select method_id from github_method_details"
                  + ");");

      // for each method, extract GitHub details using its method url and backfill that data
      while (methodIdResultSet.next()) {
        String methodId = methodIdResultSet.getString(1);

        // fetch method url and 'method_version_id' for the method
        String methodVersionQuery =
            "select method_version_id, method_version_url from method_version where method_id = ?";
        PreparedStatement methodVersionStmt = connection.prepareStatement(methodVersionQuery);
        methodVersionStmt.setObject(1, UUID.fromString(methodId));

        ResultSet methodVersionResultSet = methodVersionStmt.executeQuery();

        if (methodVersionResultSet.next()) {
          // extract GitHub details for each method
          String rawUrl = convertGithubToRawUrl(methodVersionResultSet.getString(2));
          GithubUrlComponents githubMethodDetails = extractGithubDetailsFromUrl(rawUrl);

          // create insert statement for inserting data in 'github_method_details' table
          InsertStatement insertGithubDetailsStmt =
              new InsertStatement(
                      database.getLiquibaseCatalogName(),
                      database.getLiquibaseSchemaName(),
                      "github_method_details")
                  .addColumnValue("repository", githubMethodDetails.repo())
                  .addColumnValue("organization", githubMethodDetails.org())
                  .addColumnValue("path", githubMethodDetails.path())
                  .addColumnValue(
                      "private",
                      false) // this is false since all methods until now should have been public
                  .addColumnValue("method_id", methodId);

          // create update statement for inserting value into 'branch_or_tag_name'column in
          // 'method_version' table
          UpdateStatement updateBranchTagStmt =
              new UpdateStatement(
                      database.getLiquibaseCatalogName(),
                      database.getLiquibaseSchemaName(),
                      "method_version")
                  .addNewColumnValue("branch_or_tag_name", githubMethodDetails.branchOrTag())
                  .setWhereClause(
                      database.escapeObjectName("method_version_id", LiquibaseColumn.class)
                          + " = ? ")
                  .addWhereParameters(methodVersionResultSet.getString(1));

          sqlStatementList.add(insertGithubDetailsStmt);
          sqlStatementList.add(updateBranchTagStmt);
        } else {
          // it's an error case if a method doesn't have a method url
          throw new Exception("No method url found for the method ID '%s'".formatted(methodId));
        }

        methodVersionStmt.close();
      }

      methodIdStmt.close();

      return sqlStatementList.toArray(sqlStatementArray);
    } catch (Exception e) {
      throw new CustomChangeException(e);
    }
  }

  @Override
  public String getConfirmationMessage() {
    return "Successfully back filled data (if any) to 'github_method_details' table.";
  }

  @Override
  public void setUp() throws SetupException {}

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {
    // do nothing since we don't need additional resources
  }
}
