package bio.terra.cbas.runsets.outputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.exceptions.WorkflowOutputNotFoundException;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.junit.jupiter.api.Test;

class TestOutputGenerator {
  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private List<WorkflowOutputDefinition> singleMockOutputDefinitions(
      String outputType, String recordAttribute) throws Exception {
    String rawOutputDefinition =
        """
        [
          {
            "output_name":"myWorkflow.out",
            "output_type":"%s",
            "record_attribute":"%s"
          }
        ]
        """
            .formatted(outputType, recordAttribute)
            .stripIndent()
            .trim();

    return objectMapper.readValue(rawOutputDefinition, new TypeReference<>() {});
  }

  @Test
  void stringOutput() throws Exception {
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", "Harry Potter");
    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");

    RecordAttributes actual =
        OutputGenerator.buildOutputs(
            singleMockOutputDefinitions("String", "foo_name"), cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void intOutput() throws Exception {
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", 123);
    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_id", 123);

    RecordAttributes actual =
        OutputGenerator.buildOutputs(singleMockOutputDefinitions("Int", "foo_id"), cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void booleanOutput() throws Exception {
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", true);
    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_valid", true);

    RecordAttributes actual =
        OutputGenerator.buildOutputs(
            singleMockOutputDefinitions("Boolean", "foo_valid"), cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void floatOutput() throws Exception {
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", 8.5);
    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_rating", 8.5);

    RecordAttributes actual =
        OutputGenerator.buildOutputs(
            singleMockOutputDefinitions("Float", "foo_rating"), cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void twoOutputs() throws Exception {
    String rawOutputDefinition =
        """
        [
          {
            "output_name":"myWorkflow.name",
            "output_type":"String",
            "record_attribute":"foo_name"
          },
          {
            "output_name":"myWorkflow.rating",
            "output_type":"Float",
            "record_attribute":"foo_rating"
          }
        ]
        """
            .stripIndent()
            .trim();

    List<WorkflowOutputDefinition> outputDefinitions =
        objectMapper.readValue(rawOutputDefinition, new TypeReference<>() {});

    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.name", "Harry Potter");
    cromwellOutputs.put("myWorkflow.rating", 8.5);
    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");
    expected.put("foo_rating", 8.5);

    RecordAttributes actual = OutputGenerator.buildOutputs(outputDefinitions, cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void invalidOutputName() throws Exception {
    String rawOutputDefinition =
        """
        [
          {
            "output_name":"myWorkflow.naem",
            "output_type":"String",
            "record_attribute":"foo_name"
          }
        ]
        """
            .stripIndent()
            .trim();

    List<WorkflowOutputDefinition> outputDefinitions =
        objectMapper.readValue(rawOutputDefinition, new TypeReference<>() {});

    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.name", "Harry Potter");
    cromwellOutputs.put("myWorkflow.rating", 8.5);

    Exception exception = null;
    try {
      OutputGenerator.buildOutputs(outputDefinitions, cromwellOutputs);
    } catch (Exception e) {
      exception = e;
    }

    assertNotNull(exception);
    assertTrue(exception instanceof WorkflowOutputNotFoundException);
    assertEquals(exception.getMessage(), "Output myWorkflow.naem not found in workflow outputs.");
  }
}
