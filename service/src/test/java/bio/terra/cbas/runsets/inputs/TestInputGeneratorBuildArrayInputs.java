package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithArrayFooRatingParameter;
import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithArrayLiteral;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.emptyRecord;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.runsets.types.CoercionException;
import bio.terra.cbas.runsets.types.ValueCoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildArrayInputs {

  @Test
  void zeroElementArrayLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(false, "String")),
            wdsRecordWithFooRating("[]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of());
    assertEquals(expected, actual);
  }

  @Test
  void zeroElementArrayLiteral()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayLiteral(false, "String", "[]")), emptyRecord());

    Map<String, Object> expected = Map.of("lookup_foo", List.of());
    assertEquals(expected, actual);
  }

  @Test
  void oneElementArrayLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(false, "String")),
            wdsRecordWithFooRating("[ \"exquisite\" ]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite"));
    assertEquals(expected, actual);
  }

  @Test
  void oneElementArrayLiteral()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayLiteral(false, "String", "[ \"exquisite\" ]")),
            emptyRecord());

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite"));
    assertEquals(expected, actual);
  }

  @Test
  void twoElementArrayLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(false, "String")),
            wdsRecordWithFooRating("[ \"exquisite\", \"wonderful\" ]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite", "wonderful"));
    assertEquals(expected, actual);
  }

  @Test
  void twoElementArrayLiteral()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                inputDefinitionWithArrayLiteral(
                    false, "String", "[ \"exquisite\", \"wonderful\" ]")),
            emptyRecord());

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite", "wonderful"));
    assertEquals(expected, actual);
  }

  @Test
  void zeroElementNonEmptyArrayLookup() {
    assertThrows(
        ValueCoercionException.class,
        () ->
            InputGenerator.buildInputs(
                List.of(inputDefinitionWithArrayFooRatingParameter(true, "String")),
                wdsRecordWithFooRating("[]")));
  }

  @Test
  void zeroElementNonEmptyArrayLiteral() {
    assertThrows(
        ValueCoercionException.class,
        () ->
            InputGenerator.buildInputs(
                List.of(inputDefinitionWithArrayLiteral(true, "String", "[]")), emptyRecord()));
  }

  @Test
  void twoElementNonEmptyArrayLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(true, "String")),
            wdsRecordWithFooRating("[ \"exquisite\", \"wonderful\" ]"));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite", "wonderful"));
    assertEquals(expected, actual);
  }

  @Test
  void twoElementNonEmptyArrayLiteral()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                inputDefinitionWithArrayLiteral(
                    true, "String", "[ \"exquisite\", \"wonderful\" ]")),
            emptyRecord());

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite", "wonderful"));
    assertEquals(expected, actual);
  }

  @Test
  void autoBoxingSingleElementIntoArrayFromLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithArrayFooRatingParameter(false, "String")),
            wdsRecordWithFooRating("\"exquisite\""));

    Map<String, Object> expected = Map.of("lookup_foo", List.of("exquisite"));
    assertEquals(expected, actual);
  }
}
