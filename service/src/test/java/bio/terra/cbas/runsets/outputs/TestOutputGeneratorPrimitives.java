package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.primitiveOutputDefinition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.exceptions.WorkflowOutputNotFoundException;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.junit.jupiter.api.Test;

class TestOutputGeneratorPrimitives {
  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void stringOutput() throws Exception {
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", "Harry Potter");
    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");

    RecordAttributes actual =
        OutputGenerator.buildOutputs(
            List.of(primitiveOutputDefinition("myWorkflow.out", "String", "foo_name")),
            cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void intOutput() throws Exception {
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", 123);
    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_id", 123);

    RecordAttributes actual =
        OutputGenerator.buildOutputs(
            List.of(primitiveOutputDefinition("myWorkflow.out", "Int", "foo_id")), cromwellOutputs);
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
            List.of(primitiveOutputDefinition("myWorkflow.out", "Boolean", "foo_valid")),
            cromwellOutputs);
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
            List.of(primitiveOutputDefinition("myWorkflow.out", "Float", "foo_rating")),
            cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void twoOutputs() throws Exception {
    List<WorkflowOutputDefinition> outputDefinitions =
        List.of(
            primitiveOutputDefinition("myWorkflow.name", "String", "foo_name"),
            primitiveOutputDefinition("myWorkflow.rating", "Float", "foo_rating"));

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
    List<WorkflowOutputDefinition> outputDefinitions =
        List.of(primitiveOutputDefinition("myWorkflow.naem", "String", "foo_name"));

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
