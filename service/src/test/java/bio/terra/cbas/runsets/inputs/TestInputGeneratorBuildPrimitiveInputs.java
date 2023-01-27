package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.fooRatingLiteralParameter;
import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.fooRatingRecordLookupParameter;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.emptyRecord;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.runsets.types.CoercionException;
import bio.terra.cbas.runsets.types.ValueCoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildPrimitiveInputs {

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void stringLiteral() throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingLiteralParameter("String", "\"hello world\"")), emptyRecord());
    assertEquals(Map.of("literal_foo", "hello world"), actual);
  }

  @Test
  void intLiteral() throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(List.of(fooRatingLiteralParameter("Int", "1")), emptyRecord());
    assertEquals(Map.of("literal_foo", 1L), actual);
  }

  @Test
  void booleanLiteral()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingLiteralParameter("Boolean", "false")), emptyRecord());
    assertEquals(Map.of("literal_foo", false), actual);
  }

  @Test
  void floatLiteral() throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingLiteralParameter("Float", "1.1")), emptyRecord());
    assertEquals(Map.of("literal_foo", 1.1), actual);
  }

  @Test
  void validFileLiteral()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingLiteralParameter("File", "\"gs://bucket/file.txt\"")), emptyRecord());
    assertEquals(Map.of("literal_foo", "gs://bucket/file.txt"), actual);
  }

  @Test
  void invalidFileLiteral() {

    assertThrows(
        ValueCoercionException.class,
        () ->
            InputGenerator.buildInputs(
                List.of(fooRatingLiteralParameter("File", "\"not a file\"")), emptyRecord()));
  }

  @Test
  void stringRecordLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("String")),
            wdsRecordWithFooRating("\"exquisite\""));
    assertEquals(Map.of("lookup_foo", "exquisite"), actual);
  }

  @Test
  void numberRecordLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Int")), wdsRecordWithFooRating("1000"));
    assertEquals(Map.of("lookup_foo", 1000L), actual);
  }

  @Test
  void booleanRecordLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Boolean")), wdsRecordWithFooRating("true"));
    assertEquals(Map.of("lookup_foo", true), actual);
  }

  @Test
  void floatRecordLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Float")), wdsRecordWithFooRating("1000.0001"));
    assertEquals(Map.of("lookup_foo", 1000.0001), actual);
  }

  @Test
  void mixedLiteralAndLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
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

  @Test
  void invalidAttribute() throws JsonProcessingException {
    String incorrectAttributeDefinition =
        """
            [
              {
                "input_name": "lookup_foo",
                "input_type": { "type": "primitive", "primitive_type": "String" },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "foo_rating"
                }
              }
            ]
          """
            .stripIndent()
            .trim();
    RecordResponse recordResponse =
        objectMapper.readValue(
            """
        {
          "id": "FOO1",
          "type": "FOO",
          "attributes": {
            "MY_RECORD_ATTRIBUTE": "Hello, world"
          }
        }
        """
                .trim()
                .stripIndent(),
            RecordResponse.class);
    List<WorkflowInputDefinition> inputDefinitions =
        objectMapper.readValue(incorrectAttributeDefinition, new TypeReference<>() {});
    InputProcessingException thrown =
        assertThrows(
            InputProcessingException.class,
            () -> InputGenerator.buildInputs(inputDefinitions, recordResponse),
            "Expected buildInputs() to throw and error, but didn't.");
    assertTrue(
        thrown
            .getMessage()
            .contains(
                "Attribute %s not found in WDS record %s (to populate workflow input %s)."
                    .formatted("foo_rating", "FOO1", "lookup_foo")));
  }
}
