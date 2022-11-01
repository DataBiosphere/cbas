package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.fooRatingLiteralParameter;
import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.fooRatingRecordLookupParameter;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.emptyRecord;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildPrimitiveInputs {

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void stringLiteral() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingLiteralParameter("String", "\"hello world\"")), emptyRecord());
    assertEquals(Map.of("literal_foo", "hello world"), actual);
  }

  @Test
  void intLiteral() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(List.of(fooRatingLiteralParameter("Int", "1")), emptyRecord());
    assertEquals(Map.of("literal_foo", 1), actual);
  }

  @Test
  void booleanLiteral() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingLiteralParameter("Boolean", "false")), emptyRecord());
    assertEquals(Map.of("literal_foo", false), actual);
  }

  @Test
  void floatLiteral() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingLiteralParameter("Float", "1.1")), emptyRecord());
    assertEquals(Map.of("literal_foo", 1.1), actual);
  }

  @Test
  void stringRecordLookup() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("String")),
            wdsRecordWithFooRating("\"exquisite\""));
    assertEquals(Map.of("lookup_foo", "exquisite"), actual);
  }

  @Test
  void numberRecordLookup() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Int")), wdsRecordWithFooRating("1000"));
    assertEquals(Map.of("lookup_foo", 1000), actual);
  }

  @Test
  void booleanRecordLookup() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Boolean")), wdsRecordWithFooRating("true"));
    assertEquals(Map.of("lookup_foo", true), actual);
  }

  @Test
  void floatRecordLookup() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Float")), wdsRecordWithFooRating("1000.0001"));
    assertEquals(Map.of("lookup_foo", 1000.0001), actual);
  }

  @Test
  void mixedLiteralAndLookup() throws JsonProcessingException, CoercionException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                fooRatingRecordLookupParameter("String"),
                fooRatingLiteralParameter("String", "\"hello world\"")),
            wdsRecordWithFooRating("\"exquisite\""));
    assertEquals(
        Map.of(
            "literal_foo", "hello world",
            "lookup_foo", "exquisite"),
        actual);
  }
}
