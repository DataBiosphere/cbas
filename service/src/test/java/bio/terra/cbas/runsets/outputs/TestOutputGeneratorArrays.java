package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.EngineOutputValueGenerator.multipleCromwellOutputs;
import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.arrayOutputDefinition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.google.gson.Gson;
import cromwell.client.JSON;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.junit.jupiter.api.Test;

class TestOutputGeneratorArrays {

  private final MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);

  public TestOutputGeneratorArrays() {
    JSON.setGson(new Gson());
  }

  private static List<WorkflowOutputDefinition> arrayOutputDefinitions() throws Exception {
    return List.of(
        arrayOutputDefinition("myWorkflow.look_and_say", "Int", "look_and_say"),
        arrayOutputDefinition(
            "myWorkflow.out_empty_array", "String", "sir_not_appearing_in_this_record"));
  }

  @Test
  void stringArrayOutputs() throws Exception {

    Object cromwellOutputs =
        multipleCromwellOutputs(
            Map.of(
                "myWorkflow.look_and_say", "[1, 11, 21, 1211, 111221, 312211, 13112221]",
                "myWorkflow.out_empty_array", "[]"));

    RecordAttributes actual =
        OutputGenerator.buildOutputs(arrayOutputDefinitions(), cromwellOutputs, micrometerMetrics);

    RecordAttributes expected = new RecordAttributes();
    expected.put("look_and_say", List.of(1L, 11L, 21L, 1211L, 111221L, 312211L, 13112221L));
    expected.put("sir_not_appearing_in_this_record", List.of());

    assertEquals(expected, actual);
  }
}
