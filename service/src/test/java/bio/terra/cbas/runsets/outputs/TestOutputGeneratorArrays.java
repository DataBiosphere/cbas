package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.arrayOutputDefinition;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.junit.jupiter.api.Test;

class TestOutputGeneratorArrays {
  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static List<WorkflowOutputDefinition> optionalOutputDefinitions() throws Exception {
    return List.of(
        arrayOutputDefinition("myWorkflow.look_and_say", "Int", "look_and_say"),
        arrayOutputDefinition(
            "myWorkflow.out_empty_array", "String", "sir_not_appearing_in_this_record"));
  }

  @Test
  void stringArrayOutputs() throws Exception {

    Map<String, Object> cromwellOutputs = new HashMap<>();
    cromwellOutputs.put(
        "myWorkflow.look_and_say", List.of(1, 11, 21, 1211, 111221, 312211, 13112221));
    cromwellOutputs.put("myWorkflow.out_empty_array", List.of());

    RecordAttributes actual =
        OutputGenerator.buildOutputs(optionalOutputDefinitions(), cromwellOutputs);

    RecordAttributes expected = new RecordAttributes();
    expected.put("look_and_say", List.of(1L, 11L, 21L, 1211L, 111221L, 312211L, 13112221L));
    expected.put("sir_not_appearing_in_this_record", List.of());

    assertEquals(expected, actual);
  }
}
