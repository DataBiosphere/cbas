package bio.terra.cbas.util.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;

import bio.terra.cbas.util.BackfillGithubMethodDetails;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TestBackfillGithubMethodDetails {

  BackfillGithubMethodDetails backfillGithubDetails = new BackfillGithubMethodDetails();

  PostgresDatabase mockDb = new PostgresDatabase();
  JdbcConnection jdbcConnection = Mockito.mock(JdbcConnection.class);
  Statement statement = Mockito.mock(Statement.class);

  UUID methodId1 = UUID.randomUUID();
  UUID methodId2 = UUID.randomUUID();
  UUID methodVersionId1 = UUID.randomUUID();
  UUID methodVersionId2 = UUID.randomUUID();

  @Test
  void check() throws CustomChangeException, SQLException, DatabaseException {
    ResultSet setupResultSet = Mockito.mock(ResultSet.class);

    Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
    Mockito.when(statement.executeQuery(any())).thenReturn(setupResultSet);

    mockDb.setConnection(jdbcConnection);

    // mock result set for method IDs that needs Github method details backfilled

    ResultSet methodIdResultSet = Mockito.mock(ResultSet.class);
    Mockito.when(methodIdResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
    Mockito.when(methodIdResultSet.getString(1))
        .thenReturn(methodId1.toString())
        .thenReturn(methodId2.toString());

    Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
    Mockito.when(statement.executeQuery(contains("select method_id from method")))
        .thenReturn(methodIdResultSet);

    ResultSet methodVersionResultSet1 = Mockito.mock(ResultSet.class);
    Mockito.when(methodVersionResultSet1.next()).thenReturn(true).thenReturn(false);
    Mockito.when(methodVersionResultSet1.getString(1)).thenReturn(methodVersionId1.toString());
    Mockito.when(methodVersionResultSet1.getString(2))
        .thenReturn(
            "https://github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl");

    ResultSet methodVersionResultSet2 = Mockito.mock(ResultSet.class);
    Mockito.when(methodVersionResultSet2.next()).thenReturn(true).thenReturn(false);
    Mockito.when(methodVersionResultSet2.getString(1)).thenReturn(methodVersionId2.toString());
    Mockito.when(methodVersionResultSet2.getString(2))
        .thenReturn(
            "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/v2.1.33.16/pipes/WDL/workflows/fetch_sra_to_bam.wdl");

    PreparedStatement prepareStatement = Mockito.mock(PreparedStatement.class);
    Mockito.when(jdbcConnection.prepareStatement(any())).thenReturn(prepareStatement);
    Mockito.when(prepareStatement.executeQuery())
        .thenReturn(methodVersionResultSet1)
        .thenReturn(methodVersionResultSet2);

    SqlStatement[] array = backfillGithubDetails.generateStatements(mockDb);

    assertEquals(array.length, 4);

    InsertStatement insertStatement1 = (InsertStatement) array[0];
    Map<String, Object> insertStmtValues1 = insertStatement1.getColumnValues();
    assertEquals("cromwell", insertStmtValues1.get("repository"));
    assertEquals("broadinstitute", insertStmtValues1.get("organization"));
    assertEquals(
        "centaur/src/main/resources/standardTestCases/hello/hello.wdl",
        insertStmtValues1.get("path"));
    assertEquals(false, insertStmtValues1.get("private"));
    assertEquals(methodId1.toString(), insertStmtValues1.get("method_id"));

    UpdateStatement updateStatement1 = (UpdateStatement) array[1];
    assertEquals("develop", updateStatement1.getNewColumnValues().get("branch_or_tag_name"));
    assertEquals(methodVersionId1.toString(), updateStatement1.getWhereParameters().get(0));

    InsertStatement insertStatement2 = (InsertStatement) array[2];
    Map<String, Object> insertStmtValues2 = insertStatement2.getColumnValues();
    assertEquals("viral-pipelines", insertStmtValues2.get("repository"));
    assertEquals("broadinstitute", insertStmtValues2.get("organization"));
    assertEquals("pipes/WDL/workflows/fetch_sra_to_bam.wdl", insertStmtValues2.get("path"));
    assertEquals(false, insertStmtValues2.get("private"));
    assertEquals(methodId2.toString(), insertStmtValues2.get("method_id"));

    UpdateStatement updateStatement2 = (UpdateStatement) array[3];
    assertEquals("v2.1.33.16", updateStatement2.getNewColumnValues().get("branch_or_tag_name"));
    assertEquals(methodVersionId2.toString(), updateStatement2.getWhereParameters().get(0));
  }
}
