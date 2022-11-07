package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithArrayFooRatingParameter;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.cbas.common.exceptions.WorkflowAttributesNotFoundException;
import bio.terra.cbas.runsets.types.CoercionException;
import bio.terra.cbas.runsets.types.ValueCoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildArrayInputs {

  @Test
  void zeroElementArray()
      throws JsonProcessingException, CoercionException, WorkflowAttributesNotFoundException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(false, "String")),
            wdsRecordWithFooRating("[]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of());
    assertEquals(expected, actual);
  }

  @Test
  void oneElementArray()
      throws JsonProcessingException, CoercionException, WorkflowAttributesNotFoundException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(false, "String")),
            wdsRecordWithFooRating("[ \"exquisite\" ]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite"));
    assertEquals(expected, actual);
  }

  @Test
  void twoElementArray()
      throws JsonProcessingException, CoercionException, WorkflowAttributesNotFoundException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(false, "String")),
            wdsRecordWithFooRating("[ \"exquisite\", \"wonderful\" ]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite", "wonderful"));
    assertEquals(expected, actual);
  }

  @Test
  void zeroElementNonEmptyArray() {
    assertThrows(
        ValueCoercionException.class,
        () ->
            InputGenerator.buildInputs(
                List.of(inputDefinitionWithArrayFooRatingParameter(true, "String")),
                wdsRecordWithFooRating("[]")));
  }

  @Test
  void twoElementNonEmptyArray()
      throws JsonProcessingException, CoercionException, WorkflowAttributesNotFoundException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(true, "String")),
            wdsRecordWithFooRating("[ \"exquisite\", \"wonderful\" ]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite", "wonderful"));
    assertEquals(expected, actual);
  }
}
