package bio.terra.cbas.util.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import bio.terra.cbas.util.BackfillGithubMethodDetails;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.statement.SqlStatement;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TestBackfillGithubMethodDetails {

  public BackfillGithubMethodDetails backfillGithubDetails = new BackfillGithubMethodDetails();

  JdbcConnection jdbcConnection = Mockito.mock(JdbcConnection.class);
  Statement statement = Mockito.mock(Statement.class);
  ResultSet resultSet = Mockito.mock(ResultSet.class);

  @Test
  void check() throws CustomChangeException, SQLException, DatabaseException {
    Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
    Mockito.when(statement.executeQuery(any())).thenReturn(resultSet);

    PostgresDatabase pg = new PostgresDatabase();
    pg.setConnection(jdbcConnection);

    ResultSet resultSet2 = Mockito.mock(ResultSet.class);
    Mockito.when(resultSet2.next()).thenReturn(true);
    Mockito.when(resultSet2.getString(1)).thenReturn("abc-123");

    Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
    Mockito.when(
            statement.executeQuery(
                "select method_id from method "
                    + "where method_source='GitHub'"
                    + "and method_id not in ("
                    + "select method_id from github_method_details"
                    + ");"))
        .thenReturn(resultSet2);

    SqlStatement[] array = backfillGithubDetails.generateStatements(pg);
    assertEquals(array.length, 0);
  }
}
