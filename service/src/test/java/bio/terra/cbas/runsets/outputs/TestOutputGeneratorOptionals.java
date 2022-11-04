package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.optionalOutputDefinition;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.junit.jupiter.api.Test;

class TestOutputGeneratorOptionals {
  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static List<WorkflowOutputDefinition> optionalOutputDefinitions() throws Exception {
    return List.of(
        optionalOutputDefinition("myWorkflow.out", "String", "foo_name"),
        optionalOutputDefinition(
            "myWorkflow.not_out", "String", "sir_not_appearing_in_this_record"));
  }

  @Test
  void optionalStringOutputNotReturned() throws Exception {

    // No output from Cromwell for 'not_out':
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", "Harry Potter");

    RecordAttributes actual =
        OutputGenerator.buildOutputs(optionalOutputDefinitions(), cromwellOutputs);

    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");
    expected.put("sir_not_appearing_in_this_record", null);

    assertEquals(expected, actual);
  }

  @Test
  void optionalStringOutputReturnedAsNull() throws Exception {

    // No null response from Cromwell for 'not_out':
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", "Harry Potter");
    cromwellOutputs.put("myWorkflow.not_out", null);

    RecordAttributes actual =
        OutputGenerator.buildOutputs(optionalOutputDefinitions(), cromwellOutputs);

    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");
    expected.put("sir_not_appearing_in_this_record", null);

    assertEquals(expected, actual);
  }

  @Test
  void optionalStringOutputReturnedWithValue() throws Exception {

    // No null response from Cromwell for 'not_out':
    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put("myWorkflow.out", "Harry Potter");
    cromwellOutputs.put("myWorkflow.not_out", "Tim The Sorcerer");

    RecordAttributes actual =
        OutputGenerator.buildOutputs(optionalOutputDefinitions(), cromwellOutputs);

    RecordAttributes expected = new RecordAttributes();
    expected.put("foo_name", "Harry Potter");
    expected.put("sir_not_appearing_in_this_record", "Tim The Sorcerer"); // Yeah, yeah, I know...
    assertEquals(expected, actual);
  }
}
