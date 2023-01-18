package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRunSetDao {

  @Autowired RunSetDao runSetDao;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;

  UUID runSetId = UUID.randomUUID();

  UUID methodId = UUID.randomUUID();
  UUID methodVersionId = UUID.randomUUID();

  String time = "2023-01-13T20:19:41.400292Z";
  String inputDef =
      """
            [
              {
                "input_name": "target_workflow_1.foo.input_file_1",
                "input_type": { "type": "primitive", "primitive_type": "File" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_file_1"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_file_2",
                "input_type": { "type": "primitive", "primitive_type": "File" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_file_2"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_string_1",
                "input_type": { "type": "primitive", "primitive_type": "String" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_string_1"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_string_2",
                "input_type": { "type": "primitive", "primitive_type": "String" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_string_2"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_string_3",
                "input_type": { "type": "optional", "optional_type": { "type": "primitive", "primitive_type": "String" } },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_string_3"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_string_4",
                "input_type": { "type": "primitive", "primitive_type": "String" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_string_4"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_string_5",
                "input_type": { "type": "primitive", "primitive_type": "String" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_string_5"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_string_6",
                "input_type": { "type": "primitive", "primitive_type": "String" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_string_6"
                }
              },
              {
                "input_name": "target_workflow_1.foo.input_string_7",
                "input_type": { "type": "primitive", "primitive_type": "String" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "target_workflow_1_input_string_7"
                }
              }
            ]""";

  String outputDef =
      """
            [
              {
                "output_name": "target_workflow_1.file_output",
                "output_type": { "type": "primitive", "primitive_type": "String" },
                "record_attribute": "target_workflow_1_file_output"
              }
            ]
            """;

  @Test
  void retrievesSingleRunSet() {

    Method method =
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Target Workflow 1",
            "Target Workflow 1",
            OffsetDateTime.parse("2023-01-10T16:46:23.946326Z"),
            null,
            "Github");

    MethodVersion methodVersion =
        new MethodVersion(
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            method,
            "1.0",
            "First version of target workflow 1",
            OffsetDateTime.parse("2023-01-10T16:46:23.955430Z"),
            null,
            "https://raw.githubusercontent.com/DataBiosphere/cbas/main/useful_workflows/target_workflow_1/target_workflow_1.wdl");

    RunSet runSet =
        new RunSet(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            methodVersion,
            "Target workflow 1, run 1",
            "Example run for target workflow 1",
            true,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.parse("2023-01-10T16:46:23.950968Z"),
            OffsetDateTime.parse("2023-01-10T16:46:23.950968Z"),
            OffsetDateTime.parse("2023-01-10T16:46:23.950968Z"),
            0,
            0,
            inputDef + "\n", // Compensating for the additional newline the db stores
            outputDef,
            "FOO");

    RunSet expected = runSetDao.getRunSet(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    assertEquals(runSet, expected);
  }

  @Test
  void retrievesAllRunSets() {

    List<RunSet> runSets = runSetDao.getRunSets();
    assertEquals(7, runSets.size());
  }
}
