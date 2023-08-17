package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.EngineOutputValueGenerator.multipleCromwellOutputs;
import static bio.terra.cbas.runsets.outputs.EngineOutputValueGenerator.singleCromwellOutput;
import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.primitiveOutputDefinition;
import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.primitiveOutputDefinitionToNone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.exceptions.OutputProcessingException.WorkflowOutputNotFoundException;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.runsets.types.TypeCoercionException;
import bio.terra.cbas.runsets.types.ValueCoercionException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import cromwell.client.JSON;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestOutputGeneratorPrimitives {

  public TestOutputGeneratorPrimitives() {
    JSON.setGson(new Gson());
  }

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void stringOutput() throws Exception {
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "\"Harry Potter\"");
    Map<String, Object> expected = new HashMap<>();
    expected.put("foo_name", "Harry Potter");

    Map<String, Object> actual =
        OutputGenerator.buildOutputs(
            List.of(primitiveOutputDefinition("myWorkflow.out", "String", "foo_name")),
            cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void intOutput() throws Exception {
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "123");
    Map<String, Object> expected = new HashMap<>();
    expected.put("foo_id", 123L);

    Map<String, Object> actual =
        OutputGenerator.buildOutputs(
            List.of(primitiveOutputDefinition("myWorkflow.out", "Int", "foo_id")), cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void invalidIntOutput() throws Exception {
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "123.4");

    Assertions.assertThrows(
        TypeCoercionException.class,
        () ->
            OutputGenerator.buildOutputs(
                List.of(primitiveOutputDefinition("myWorkflow.out", "Int", "foo_id")),
                cromwellOutputs));
  }

  @Test
  void booleanOutput() throws Exception {
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "true");
    Map<String, Object> expected = new HashMap<>();
    expected.put("foo_valid", true);

    Map<String, Object> actual =
        OutputGenerator.buildOutputs(
            List.of(primitiveOutputDefinition("myWorkflow.out", "Boolean", "foo_valid")),
            cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void floatOutput() throws Exception {
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "8.5");
    Map<String, Object> expected = new HashMap<>();
    expected.put("foo_rating", 8.5);

    Map<String, Object> actual =
        OutputGenerator.buildOutputs(
            List.of(primitiveOutputDefinition("myWorkflow.out", "Float", "foo_rating")),
            cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void validFileOutput() throws Exception {
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "\"gs://bucket/file-out\"");

    List<WorkflowOutputDefinition> outputDefinitions =
        List.of(primitiveOutputDefinition("myWorkflow.out", "File", "foo_rating"));

    Map<String, Object> expected = new HashMap<>();
    expected.put("foo_rating", "gs://bucket/file-out");

    Map<String, Object> actual = OutputGenerator.buildOutputs(outputDefinitions, cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void invalidFileOutput() throws Exception {
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "\"not a file\"");

    List<WorkflowOutputDefinition> outputDefinitions =
        List.of(primitiveOutputDefinition("myWorkflow.out", "File", "foo_rating"));

    Assertions.assertThrows(
        ValueCoercionException.class,
        () -> OutputGenerator.buildOutputs(outputDefinitions, cromwellOutputs));
  }

  @Test
  void twoOutputs() throws Exception {
    List<WorkflowOutputDefinition> outputDefinitions =
        List.of(
            primitiveOutputDefinition("myWorkflow.name", "String", "foo_name"),
            primitiveOutputDefinition("myWorkflow.rating", "Float", "foo_rating"));

    Object cromwellOutputs =
        multipleCromwellOutputs(
            Map.of("myWorkflow.name", "\"Harry Potter\"", "myWorkflow.rating", "8.5"));

    Map<String, Object> expected = new HashMap<>();
    expected.put("foo_name", "Harry Potter");
    expected.put("foo_rating", 8.5);

    Map<String, Object> actual = OutputGenerator.buildOutputs(outputDefinitions, cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void oneUsedAndOneIgnoredOutput() throws Exception {
    List<WorkflowOutputDefinition> outputDefinitions =
        List.of(
            primitiveOutputDefinition("myWorkflow.name", "String", "foo_name"),
            primitiveOutputDefinitionToNone("myWorkflow.rating", "Float"));

    Object cromwellOutputs =
        multipleCromwellOutputs(
            Map.of("myWorkflow.name", "\"Harry Potter\"", "myWorkflow.rating", "8.5"));

    Map<String, Object> expected = new HashMap<>();
    expected.put("foo_name", "Harry Potter");

    Map<String, Object> actual = OutputGenerator.buildOutputs(outputDefinitions, cromwellOutputs);
    assertEquals(expected, actual);
  }

  @Test
  void invalidOutputName() throws Exception {
    List<WorkflowOutputDefinition> outputDefinitions =
        List.of(primitiveOutputDefinition("myWorkflow.naem", "String", "foo_name"));

    Object cromwellOutputs =
        multipleCromwellOutputs(
            Map.of("myWorkflow.name", "\"Harry Potter\"", "myWorkflow.rating", "8.5"));

    Exception exception = null;
    try {
      OutputGenerator.buildOutputs(outputDefinitions, cromwellOutputs);
    } catch (Exception e) {
      exception = e;
    }

    assertNotNull(exception);
    assertTrue(exception instanceof WorkflowOutputNotFoundException);
    assertEquals("Output myWorkflow.naem not found in workflow outputs.", exception.getMessage());
  }
}
