package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.EngineOutputValueGenerator.singleCromwellOutput;
import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.mapOutputDefinition;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.google.gson.Gson;
import cromwell.client.JSON;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestOutputGeneratorMaps {
  public TestOutputGeneratorMaps() {
    JSON.setGson(new Gson());
  }

  private static List<WorkflowOutputDefinition> mapOutputDefinitions() throws Exception {
    return List.of(mapOutputDefinition("myWorkflow.look_and_say", "String", "Int", "look_and_say"));
  }

  @Test
  void stringArrayOutputs() throws Exception {

    Object cromwellOutputs =
        singleCromwellOutput("myWorkflow.look_and_say", "{ \"one\": 1, \"two\": 2 }");

    Map<String, Object> actual =
        OutputGenerator.buildOutputs(mapOutputDefinitions(), cromwellOutputs);

    Map<String, Object> expected = new HashMap<>();
    expected.put("look_and_say", Map.of("one", 1L, "two", 2L));

    assertEquals(expected, actual);
  }
}
