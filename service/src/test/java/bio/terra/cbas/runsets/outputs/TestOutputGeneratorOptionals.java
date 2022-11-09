package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.EngineOutputValueGenerator.multipleCromwellOutputs;
import static bio.terra.cbas.runsets.outputs.EngineOutputValueGenerator.singleCromwellOutput;
import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.optionalOutputDefinition;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import cromwell.client.JSON;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.junit.jupiter.api.Test;

class TestOutputGeneratorOptionals {

  public TestOutputGeneratorOptionals() {
    JSON.setGson(new Gson());
  }

  private static List<WorkflowOutputDefinition> optionalOutputDefinitions() throws Exception {
    return List.of(
        optionalOutputDefinition("myWorkflow.out", "String", "foo_name"),
        optionalOutputDefinition(
            "myWorkflow.not_out", "String", "sir_not_appearing_in_this_record"));
  }

  @Test
  void optionalStringOutputNotReturned() throws Exception {

    // No output from Cromwell for 'not_out':
    Object cromwellOutputs = singleCromwellOutput("myWorkflow.out", "\"Harry Potter\"");

    RecordAttributes actual =
        OutputGenerator.buildOutputs(optionalOutputDefinitions(), cromwellOutputs);

    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");
    expected.put("sir_not_appearing_in_this_record", null);

    assertEquals(expected, actual);
  }

  @Test
  void optionalStringOutputReturnedAsNull() throws Exception {

    // Null response from Cromwell for 'not_out':
    Object cromwellOutputs =
        multipleCromwellOutputs(
            Map.of("myWorkflow.out", "\"Harry Potter\"", "myworkflow.not_out", "null"));

    RecordAttributes actual =
        OutputGenerator.buildOutputs(optionalOutputDefinitions(), cromwellOutputs);

    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");
    expected.put("sir_not_appearing_in_this_record", null);

    assertEquals(expected, actual);
  }

  @Test
  void optionalStringOutputReturnedWithValue() throws Exception {

    // Included response from Cromwell for 'not_out':
    Object cromwellOutputs =
        multipleCromwellOutputs(
            Map.of(
                "myWorkflow.out",
                "\"Harry Potter\"",
                "myWorkflow.not_out",
                "\"Tim The Sorcerer\""));

    RecordAttributes actual =
        OutputGenerator.buildOutputs(optionalOutputDefinitions(), cromwellOutputs);

    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");
    expected.put("sir_not_appearing_in_this_record", "Tim The Sorcerer"); // Yeah, yeah, I know...
    assertEquals(expected, actual);
  }
}
