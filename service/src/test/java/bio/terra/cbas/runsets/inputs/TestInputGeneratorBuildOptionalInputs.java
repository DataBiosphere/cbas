package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithOptionalFooRatingParameter;
import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithOptionalNoneParameter;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.emptyRecord;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildOptionalInputs {

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void optionalStringProvided()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithOptionalFooRatingParameter("String")),
            wdsRecordWithFooRating("\"exquisite\""));
    assertEquals(Map.of("lookup_foo", "exquisite"), actual);
  }

  @Test
  void optionalStringNotProvided()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithOptionalFooRatingParameter("String")), emptyRecord());
    Map<Object, Object> expected = new HashMap<>();
    assertEquals(expected, actual);
  }

  @Test
  void optionalStringNoneSource()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithOptionalNoneParameter("String")), emptyRecord());
    Map<Object, Object> expected = new HashMap<>();
    assertEquals(expected, actual);
  }
}
