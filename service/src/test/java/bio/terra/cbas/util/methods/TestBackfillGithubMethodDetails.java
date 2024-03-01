package bio.terra.cbas.util.methods;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;

import bio.terra.cbas.util.BackfillGithubMethodDetails;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

  record TestMethodWithGithubDetails(
      String methodId,
      String methodVersionId,
      String methodUrl,
      Map<String, String> expectedSqlStmtValues) {}

  static BackfillGithubMethodDetails backfillGithubDetails = new BackfillGithubMethodDetails();

  static PostgresDatabase mockDb = new PostgresDatabase();
  static JdbcConnection jdbcConnection = Mockito.mock(JdbcConnection.class);
  static Statement statement = Mockito.mock(Statement.class);

  String methodId1 = UUID.randomUUID().toString();
  String methodId2 = UUID.randomUUID().toString();
  String methodVersionId1 = UUID.randomUUID().toString();
  String methodVersionId2 = UUID.randomUUID().toString();
  String helloMethodUrl =
      "https://github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl";
  String sraToBamMethodUrl =
      "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/v2.1.33.16/pipes/WDL/workflows/fetch_sra_to_bam.wdl";

  Map<String, String> githubDetailsForHelloMethod(String methodId, String methodVersionId) {
    return Map.of(
        "repository", "cromwell",
        "organization", "broadinstitute",
        "path", "centaur/src/main/resources/standardTestCases/hello/hello.wdl",
        "branch_or_tag_name", "develop",
        "method_id", methodId,
        "method_version_id", methodVersionId);
  }

  Map<String, String> githubDetailsForSraToBamMethod(String methodId, String methodVersionId) {
    return Map.of(
        "repository", "viral-pipelines",
        "organization", "broadinstitute",
        "path", "pipes/WDL/workflows/fetch_sra_to_bam.wdl",
        "branch_or_tag_name", "v2.1.33.16",
        "method_id", methodId,
        "method_version_id", methodVersionId);
  }

  // helper method to generate N TestMethodWithGithubDetails objects
  List<TestMethodWithGithubDetails> generateNTestMethodWithGithubDetails(int count) {
    List<TestMethodWithGithubDetails> testMethodList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String methodId = UUID.randomUUID().toString();
      String methodVersionId = UUID.randomUUID().toString();

      testMethodList.add(
          new TestMethodWithGithubDetails(
              methodId,
              methodVersionId,
              helloMethodUrl,
              githubDetailsForHelloMethod(methodId, methodVersionId)));
    }
    return testMethodList;
  }

  @TestFactory
  Stream<DynamicTest> testVariousBackfillScenariosWithoutException()
      throws DatabaseException, SQLException {

    // setup for mock database
    ResultSet setupResultSet = Mockito.mock(ResultSet.class);
    Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
    Mockito.when(statement.executeQuery(any())).thenReturn(setupResultSet);
    mockDb.setConnection(jdbcConnection);

    record TestCase(
        String testName,
        List<TestMethodWithGithubDetails> testMethodWithGithubDetails,
        int expectedSqlStmtArraySize) {
      void check() throws CustomChangeException, SQLException, DatabaseException {
        // **** ARRANGE ****//

        // setup for values to be returned in ResultSet for fetching methods whose GitHub details
        // need to be backfilled
        List<Boolean> methodResultSetNext = new ArrayList<>();
        List<String> methodResultSetIds = new ArrayList<>();
        for (TestMethodWithGithubDetails method : testMethodWithGithubDetails) {
          methodResultSetNext.add(true);
          methodResultSetIds.add(method.methodId);
        }
        methodResultSetNext.add(false);

        // create mock ResultSet for method IDs that needs GitHub method details backfilled
        ResultSet methodIdResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(methodIdResultSet.next())
            .thenAnswer(AdditionalAnswers.returnsElementsOf(methodResultSetNext));
        Mockito.when(methodIdResultSet.getString(1))
            .thenAnswer(AdditionalAnswers.returnsElementsOf(methodResultSetIds));

        // return mock ResultSet when method IDs that needs GitHub method details backfilled query
        // is executed
        Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
        Mockito.when(statement.executeQuery(contains("select method_id from method")))
            .thenReturn(methodIdResultSet);

        // create and setup mock ResultSet(s) for getting method url and method version ID details
        List<ResultSet> methodVersionResultSets = new ArrayList<>();
        for (TestMethodWithGithubDetails method : testMethodWithGithubDetails) {
          ResultSet methodVersionResultSet = Mockito.mock(ResultSet.class);
          Mockito.when(methodVersionResultSet.next()).thenReturn(true).thenReturn(false);
          Mockito.when(methodVersionResultSet.getString(1)).thenReturn(method.methodVersionId);
          Mockito.when(methodVersionResultSet.getString(2)).thenReturn(method.methodUrl);

          methodVersionResultSets.add(methodVersionResultSet);
        }

        // return mock ResultSet when query to fetch method version details is executed for each
        // method
        PreparedStatement prepareStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(jdbcConnection.prepareStatement(any())).thenReturn(prepareStatement);
        Mockito.when(prepareStatement.executeQuery())
            .thenAnswer(AdditionalAnswers.returnsElementsOf(methodVersionResultSets));

        // **** ACT ****//
        SqlStatement[] actualSqlStmtArray = backfillGithubDetails.generateStatements(mockDb);

        // **** ASSERT ****//

        // verify the size of SqlStatements array returned from method
        // for each method version, we generate 2 SqlStatements
        // - one for inserting data into 'github_method_details' table
        // - one for updating 'branch_or_tag_name' column in 'method_version' table
        assertEquals(actualSqlStmtArray.length, expectedSqlStmtArraySize);

        // verify the InsertStatement and UpdateStatement values
        for (int i = 0, j = 0;
            i < actualSqlStmtArray.length / 2 & j < testMethodWithGithubDetails.size();
            i = i + 2, j++) {
          Map<String, String> expectedValues =
              testMethodWithGithubDetails.get(j).expectedSqlStmtValues;

          InsertStatement insertStatement = (InsertStatement) actualSqlStmtArray[i];
          Map<String, Object> actualInsertStmtValues = insertStatement.getColumnValues();
          assertEquals(expectedValues.get("repository"), actualInsertStmtValues.get("repository"));
          assertEquals(
              expectedValues.get("organization"), actualInsertStmtValues.get("organization"));
          assertEquals(expectedValues.get("path"), actualInsertStmtValues.get("path"));
          assertEquals(false, actualInsertStmtValues.get("private"));
          assertEquals(expectedValues.get("method_id"), actualInsertStmtValues.get("method_id"));

          UpdateStatement updateStatement = (UpdateStatement) actualSqlStmtArray[i + 1];
          assertEquals(
              expectedValues.get("branch_or_tag_name"),
              updateStatement.getNewColumnValues().get("branch_or_tag_name"));
          assertEquals(
              expectedValues.get("method_version_id"), updateStatement.getWhereParameters().get(0));
        }
      }
    }

    TestCase[] testCases =
        new TestCase[] {
          new TestCase("no data to backfill", List.of(), 0),
          new TestCase(
              "backfill data for 1 method",
              List.of(
                  new TestMethodWithGithubDetails(
                      methodId1,
                      methodVersionId1,
                      helloMethodUrl,
                      githubDetailsForHelloMethod(methodId1, methodVersionId1))),
              2),
          new TestCase(
              "backfill data for 2 methods",
              List.of(
                  new TestMethodWithGithubDetails(
                      methodId1,
                      methodVersionId1,
                      helloMethodUrl,
                      githubDetailsForHelloMethod(methodId1, methodVersionId1)),
                  new TestMethodWithGithubDetails(
                      methodId2,
                      methodVersionId2,
                      sraToBamMethodUrl,
                      githubDetailsForSraToBamMethod(methodId2, methodVersionId2))),
              4),
          new TestCase("backfill data for 5 methods", generateNTestMethodWithGithubDetails(5), 10),
          new TestCase("backfill data for 5 methods", generateNTestMethodWithGithubDetails(10), 20),
          new TestCase(
              "backfill data for 100 methods", generateNTestMethodWithGithubDetails(100), 200)
        };

    return DynamicTest.stream(Stream.of(testCases), TestCase::testName, TestCase::check);
  }

  @Test
  void throwsExceptionForNoMethodUrl() throws DatabaseException, SQLException {
    // **** ARRANGE ****//

    // setup for mock database
    ResultSet setupResultSet = Mockito.mock(ResultSet.class);
    Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
    Mockito.when(statement.executeQuery(any())).thenReturn(setupResultSet);
    mockDb.setConnection(jdbcConnection);

    // create mock ResultSet for method ID that needs GitHub method details backfilled
    ResultSet methodIdResultSet = Mockito.mock(ResultSet.class);
    Mockito.when(methodIdResultSet.next()).thenReturn(true).thenReturn(false);
    Mockito.when(methodIdResultSet.getString(1)).thenReturn(methodId1);

    // return mock ResultSet when method ID that needs GitHub method details backfilled query
    // is executed
    Mockito.when(jdbcConnection.createStatement()).thenReturn(statement);
    Mockito.when(statement.executeQuery(contains("select method_id from method")))
        .thenReturn(methodIdResultSet);

    // return empty ResultSet when query to fetch method version details is executed for each method
    PreparedStatement prepareStatement = Mockito.mock(PreparedStatement.class);
    Mockito.when(jdbcConnection.prepareStatement(any())).thenReturn(prepareStatement);
    Mockito.when(prepareStatement.executeQuery()).thenReturn(Mockito.mock(ResultSet.class));

    // **** ACT & ASSERT ****//
    CustomChangeException exceptionThrown =
        assertThrows(
            CustomChangeException.class, () -> backfillGithubDetails.generateStatements(mockDb));

    assertThat(
        exceptionThrown.getMessage(),
        containsString("No method url found for the method ID '%s'".formatted(methodId1)));
  }
}
