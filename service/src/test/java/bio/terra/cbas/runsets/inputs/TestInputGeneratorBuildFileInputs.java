package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithArrayFooRatingParameter;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildFileInputs {

  @Test
  void oneElementArray() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter("String")),
            wdsRecordWithFooRating("[ \"exquisite\" ]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite"));
    assertEquals(expected, actual);
  }
}
