package bio.terra.cbas.runsets.inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.exceptions.WorkflowAttributesNotFoundException;
import bio.terra.cbas.model.WorkflowInputDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildInputs {

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void stringLiteral() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(literalPrimitiveFooParameter("String", "\"hello world\"")), emptyRecord());
    assertEquals(Map.of("literal_foo", "hello world"), actual);
  }

  @Test
  void intLiteral() throws JsonProcessingException, Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(literalPrimitiveFooParameter("Int", "1")), emptyRecord());
    assertEquals(Map.of("literal_foo", 1), actual);
  }

  @Test
  void booleanLiteral() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(literalPrimitiveFooParameter("Boolean", "false")), emptyRecord());
    assertEquals(Map.of("literal_foo", false), actual);
  }

  @Test
  void floatLiteral() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(literalPrimitiveFooParameter("Float", "1.1")), emptyRecord());
    assertEquals(Map.of("literal_foo", 1.1), actual);
  }

  @Test
  void stringRecordLookup() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("String")), fooRatingRecord("\"exquisite\""));
    assertEquals(Map.of("lookup_foo", "exquisite"), actual);
  }

  @Test
  void numberRecordLookup() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Int")), fooRatingRecord("1000"));
    assertEquals(Map.of("lookup_foo", 1000), actual);
  }

  @Test
  void booleanRecordLookup() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Boolean")), fooRatingRecord("true"));
    assertEquals(Map.of("lookup_foo", true), actual);
  }

  @Test
  void floatRecordLookup() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingRecordLookupParameter("Float")), fooRatingRecord("1000.0001"));
    assertEquals(Map.of("lookup_foo", 1000.0001), actual);
  }

  @Test
  void mixedLiteralAndLookup() throws Exception {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                fooRatingRecordLookupParameter("String"),
                literalPrimitiveFooParameter("String", "\"hello world\"")),
            fooRatingRecord("\"exquisite\""));
    assertEquals(
        Map.of(
            "literal_foo", "hello world",
            "lookup_foo", "exquisite"),
        actual);
  }

  // Stock Parameter definitions:
  private static WorkflowInputDefinition literalPrimitiveFooParameter(
      String parameterType, String rawLiteralJson) throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "literal_foo",
          "input_type": { "type": "primitive", "primitive_type": "%s" },
          "source": {
            "type": "literal",
            "parameter_value": %s
          }
        }"""
            .formatted(parameterType, rawLiteralJson)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  private static WorkflowInputDefinition fooRatingRecordLookupParameter(String parameterType)
      throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": { "type": "primitive", "primitive_type": "%s" },
          "source": {
            "type": "record_lookup",
            "record_attribute": "foo-rating"
          }
        }"""
            .formatted(parameterType)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  // Stock Record Responses:

  private static RecordResponse emptyRecord() throws JsonProcessingException {
    return objectMapper.readValue(
        """
        {
          "id": "FOO1",
          "type": "FOO",
          "attributes": {
          }
        }"""
            .stripIndent()
            .trim(),
        RecordResponse.class);
  }

  private static RecordResponse fooRatingRecord(String rawAttributeJson)
      throws JsonProcessingException {
    return objectMapper.readValue(
        """
        {
          "id": "FOO1",
          "type": "FOO",
          "attributes": {
            "foo-rating": %s
          }
        }"""
            .formatted(rawAttributeJson)
            .stripIndent()
            .trim(),
        RecordResponse.class);
  }

  @Test
  void invalidAttribute() throws Exception {
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

    WorkflowAttributesNotFoundException thrown =
        assertThrows(
            WorkflowAttributesNotFoundException.class,
            () -> InputGenerator.buildInputs(inputDefinitions, recordResponse),
            "Expected buildInputs() to throw and error, but didn't.");
    assertTrue(thrown.getMessage().contains("Attribute not found in WDS record."));
  }
}
