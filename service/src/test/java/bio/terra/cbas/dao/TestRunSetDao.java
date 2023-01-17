package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @ContextConfiguration(classes = RunSetDao.class)
public class TestRunSetDao {

  @Autowired RunSetDao runSetDao;
  @Autowired MethodVersionDao methodVersionDao;
  @Autowired MethodDao methodDao;
  UUID runSetId;
  String inputDef;
  String outputDef;
  UUID methodVersionId = UUID.randomUUID();
  UUID methodId = UUID.randomUUID();
  String time = "2023-01-13T20:19:41.400292Z";

  MethodVersion methodVersion;

  @BeforeEach
  void setUp() {
    Method method =
        new Method(
            methodId, "test method", "test method", OffsetDateTime.parse(time), null, "Github");

    methodDao.createMethod(method);

    methodVersion =
        new MethodVersion(
            methodVersionId,
            method,
            "1.0",
            "db test method version",
            OffsetDateTime.now(),
            null,
            "http://helloworld.com");

    methodVersionDao.createMethodVersion(methodVersion);

    runSetId = UUID.randomUUID();
    inputDef =
        """
            [
              {
                "input_name": "test_workflow_1.foo.input_file_1",
                "input_type": { "type": "primitive", "primitive_type": "File" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "test_workflow_1_input_file_1"
                }
              }
            ]""";

    outputDef =
        """
            [
              {
                "output_name": "test_workflow_1.file_output",
                "output_type": { "type": "primitive", "primitive_type": "String" },
                "record_attribute": "test_workflow_1_file_output"
              }
            ]
            """;
  }

  @Test
  void retrievesSingleRunSet() {
    UUID runSetId = UUID.randomUUID();

    RunSet testRunSet =
        new RunSet(
            runSetId,
            methodVersion,
            "Test run set",
            "A test run set for the db",
            Boolean.TRUE,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            1,
            0,
            inputDef,
            outputDef,
            "TESTFOO");

    runSetDao.createRunSet(testRunSet);

    RunSet expected = runSetDao.getRunSet(runSetId);

    assertEquals(testRunSet, expected);
  }

  @Test
  void retrievesAllRunSets() {

    List<RunSet> runSets = runSetDao.getRunSets();

    // what is pulled from the db?
    // List<RunSet> runSetsExpected = new RunSet(...)

    // assertEquals(runSetsExpected, runSets);
  }
}
