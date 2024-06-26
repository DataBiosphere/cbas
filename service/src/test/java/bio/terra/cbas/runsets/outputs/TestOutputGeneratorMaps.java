package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.runsets.outputs.EngineOutputValueGenerator.singleCromwellOutput;
import static bio.terra.cbas.runsets.outputs.StockOutputDefinitions.mapOutputDefinition;
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

class TestOutputGeneratorMaps {

  private final MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);

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

    RecordAttributes actual =
        OutputGenerator.buildOutputs(mapOutputDefinitions(), cromwellOutputs, micrometerMetrics);

    RecordAttributes expected = new RecordAttributes();
    expected.put("look_and_say", Map.of("one", 1L, "two", 2L));

    assertEquals(expected, actual);
  }
}
