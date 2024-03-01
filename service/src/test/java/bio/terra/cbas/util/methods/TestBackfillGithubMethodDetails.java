package bio.terra.cbas.util.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;

import bio.terra.cbas.util.BackfillGithubMethodDetails;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

public class TestBackfillGithubMethodDetails {

  static BackfillGithubMethodDetails backfillGithubDetails = new BackfillGithubMethodDetails();

  static PostgresDatabase mockDb = new PostgresDatabase();
  static JdbcConnection jdbcConnection = Mockito.mock(JdbcConnection.class);
  static Statement statement = Mockito.mock(Statement.class);

  String methodId1 = UUID.randomUUID().toString();
  String methodId2 = UUID.randomUUID().toString();
  String methodVersionId1 = UUID.randomUUID().toString();
  String methodVersionId2 = UUID.randomUUID().toString();

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
        .thenAnswer(AdditionalAnswers.returnsElementsOf(Arrays.asList(methodId1, methodId2)));

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
    assertEquals(methodId1, insertStmtValues1.get("method_id"));

    UpdateStatement updateStatement1 = (UpdateStatement) array[1];
    assertEquals("develop", updateStatement1.getNewColumnValues().get("branch_or_tag_name"));
    assertEquals(methodVersionId1.toString(), updateStatement1.getWhereParameters().get(0));

    InsertStatement insertStatement2 = (InsertStatement) array[2];
    Map<String, Object> insertStmtValues2 = insertStatement2.getColumnValues();
    assertEquals("viral-pipelines", insertStmtValues2.get("repository"));
    assertEquals("broadinstitute", insertStmtValues2.get("organization"));
    assertEquals("pipes/WDL/workflows/fetch_sra_to_bam.wdl", insertStmtValues2.get("path"));
    assertEquals(false, insertStmtValues2.get("private"));
    assertEquals(methodId2, insertStmtValues2.get("method_id"));

    UpdateStatement updateStatement2 = (UpdateStatement) array[3];
    assertEquals("v2.1.33.16", updateStatement2.getNewColumnValues().get("branch_or_tag_name"));
    assertEquals(methodVersionId2.toString(), updateStatement2.getWhereParameters().get(0));
  }

  @TestFactory
  Stream<DynamicTest> tableDrivenTest() {

    record MethodVersionRecord(String methodVersionId, String methodUrl) {}

    record TestCase(
        String testName,
        List<String> methodIds,
        List<MethodVersionRecord> methodVersionRecords,
        int expectedSqlStmtsSize,
        List<Map<String, Object>> expectedInsertValues,
        List<Map<String, Object>> expectedUpdateValues) {

      void check() throws CustomChangeException, SQLException, DatabaseException {
        ResultSet setupResultSet = Mockito.mock(ResultSet.class);

        Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
        Mockito.when(statement.executeQuery(any())).thenReturn(setupResultSet);

        mockDb.setConnection(jdbcConnection);

        List<Boolean> methodResultSetNext = new ArrayList<>();
        for (String ignored : methodIds) {
          methodResultSetNext.add(true);
        }
        methodResultSetNext.add(false);

        // mock result set for method IDs that needs Github method details backfilled

        ResultSet methodIdResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(methodIdResultSet.next())
            .thenAnswer(AdditionalAnswers.returnsElementsOf(methodResultSetNext));

        Mockito.when(methodIdResultSet.getString(1))
            .thenAnswer(AdditionalAnswers.returnsElementsOf(methodIds));

        Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
        Mockito.when(statement.executeQuery(contains("select method_id from method")))
            .thenReturn(methodIdResultSet);

        List<ResultSet> methodVersionResultSets = new ArrayList<>();
        for (MethodVersionRecord record : methodVersionRecords) {
          ResultSet methodVersionResultSet = Mockito.mock(ResultSet.class);
          Mockito.when(methodVersionResultSet.next()).thenReturn(true).thenReturn(false);
          Mockito.when(methodVersionResultSet.getString(1)).thenReturn(record.methodVersionId);
          Mockito.when(methodVersionResultSet.getString(2)).thenReturn(record.methodUrl);

          methodVersionResultSets.add(methodVersionResultSet);
        }

        PreparedStatement prepareStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(jdbcConnection.prepareStatement(any())).thenReturn(prepareStatement);
        Mockito.when(prepareStatement.executeQuery())
            .thenAnswer(AdditionalAnswers.returnsElementsOf(methodVersionResultSets));

        SqlStatement[] actualSqlStmtArray = backfillGithubDetails.generateStatements(mockDb);

        // for each method version, we generate 2 SqlStatements
        // - one for inserting data into 'github_method_details' table
        // - one for updating 'branch_or_tag_name' column in 'method_version' table
        assertEquals(actualSqlStmtArray.length, expectedSqlStmtsSize);

        for (int i = 0, j = 0;
            i < actualSqlStmtArray.length / 2 & j < expectedInsertValues.size();
            i = i + 2, j++) {
          InsertStatement insertStatement = (InsertStatement) actualSqlStmtArray[i];
          Map<String, Object> actualInsertStmtValues = insertStatement.getColumnValues();
          assertEquals(
              expectedInsertValues.get(j).get("repository"),
              actualInsertStmtValues.get("repository"));
          assertEquals(
              expectedInsertValues.get(j).get("organization"),
              actualInsertStmtValues.get("organization"));
          assertEquals(expectedInsertValues.get(j).get("path"), actualInsertStmtValues.get("path"));
          assertEquals(false, actualInsertStmtValues.get("private"));
          assertEquals(
              expectedInsertValues.get(j).get("method_id"),
              actualInsertStmtValues.get("method_id"));

          UpdateStatement updateStatement = (UpdateStatement) actualSqlStmtArray[i + 1];
          assertEquals(
              expectedUpdateValues.get(j).get("branch_or_tag_name"),
              updateStatement.getNewColumnValues().get("branch_or_tag_name"));
          assertEquals(
              expectedUpdateValues.get(j).get("method_version_id"),
              updateStatement.getWhereParameters().get(0));
        }
      }
    }

    var testCases =
        new TestCase[] {
          new TestCase("no data to backfill", List.of(), List.of(), 0, List.of(), List.of()),
          new TestCase(
              "backfill data for 2 methods",
              Arrays.asList(methodId1, methodId2),
              Arrays.asList(
                  new MethodVersionRecord(
                      methodVersionId1,
                      "https://github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl"),
                  new MethodVersionRecord(
                      methodVersionId2,
                      "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/v2.1.33.16/pipes/WDL/workflows/fetch_sra_to_bam.wdl")),
              4,
              Arrays.asList(
                  Map.of(
                      "repository", "cromwell",
                      "organization", "broadinstitute",
                      "path", "centaur/src/main/resources/standardTestCases/hello/hello.wdl",
                      "method_id", methodId1),
                  Map.of(
                      "repository", "viral-pipelines",
                      "organization", "broadinstitute",
                      "path", "pipes/WDL/workflows/fetch_sra_to_bam.wdl",
                      "method_id", methodId2)),
              Arrays.asList(
                  Map.of("branch_or_tag_name", "develop", "method_version_id", methodVersionId1),
                  Map.of(
                      "branch_or_tag_name", "v2.1.33.16", "method_version_id", methodVersionId2)))
        };

    return DynamicTest.stream(Stream.of(testCases), TestCase::testName, TestCase::check);
  }
}
