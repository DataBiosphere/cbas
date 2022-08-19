package bio.terra.cbas.runsets.inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.WorkflowParamDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.EntityResponse;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildInputs {

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void stringLiteral() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(literalFooParameter("String", "\"hello world\"")), emptyEntity());
    assertEquals(Map.of("literal_foo", "hello world"), actual);
  }

  @Test
  void intLiteral() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(List.of(literalFooParameter("Int", "1")), emptyEntity());
    assertEquals(Map.of("literal_foo", 1), actual);
  }

  @Test
  void booleanLiteral() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(List.of(literalFooParameter("Boolean", "false")), emptyEntity());
    assertEquals(Map.of("literal_foo", false), actual);
  }

  @Test
  void floatLiteral() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(List.of(literalFooParameter("Float", "1.1")), emptyEntity());
    assertEquals(Map.of("literal_foo", 1.1), actual);
  }

  @Test
  void stringEntityLookup() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingEntityLookupParameter("String")), fooRatingEntity("\"exquisite\""));
    assertEquals(Map.of("lookup_foo", "exquisite"), actual);
  }

  @Test
  void numberEntityLookup() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingEntityLookupParameter("Int")), fooRatingEntity("1000"));
    assertEquals(Map.of("lookup_foo", 1000), actual);
  }

  @Test
  void booleanEntityLookup() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingEntityLookupParameter("Boolean")), fooRatingEntity("true"));
    assertEquals(Map.of("lookup_foo", true), actual);
  }

  @Test
  void floatEntityLookup() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(fooRatingEntityLookupParameter("Float")), fooRatingEntity("1000.0001"));
    assertEquals(Map.of("lookup_foo", 1000.0001), actual);
  }

  @Test
  void mixedLiteralAndLookup() throws JsonProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                fooRatingEntityLookupParameter("String"),
                literalFooParameter("String", "\"hello world\"")),
            fooRatingEntity("\"exquisite\""));
    assertEquals(
        Map.of(
            "literal_foo", "hello world",
            "lookup_foo", "exquisite"),
        actual);
  }

  // Stock Parameter definitions:
  private static WorkflowParamDefinition literalFooParameter(
      String parameterType, String rawLiteralJson) throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "parameter_name": "literal_foo",
          "parameter_type": "%s",
          "source": {
            "type": "literal",
            "parameter_value": %s
          }
        }"""
            .formatted(parameterType, rawLiteralJson)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowParamDefinition.class);
  }

  private static WorkflowParamDefinition fooRatingEntityLookupParameter(String parameterType)
      throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "parameter_name": "lookup_foo",
          "parameter_type": "%s",
          "source": {
            "type": "entity_lookup",
            "entity_attribute": "foo-rating"
          }
        }"""
            .formatted(parameterType)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowParamDefinition.class);
  }

  // Stock Entity Responses:

  private static EntityResponse emptyEntity() throws JsonProcessingException {
    return objectMapper.readValue(
        """
        {
          "id": "FOO1",
          "type": "FOO",
          "attributes": {
          },
          "metadata": {
            "provenance": "TODO: ENTITYMETADATA"
          }
        }"""
            .stripIndent()
            .trim(),
        EntityResponse.class);
  }

  private static EntityResponse fooRatingEntity(String rawAttributeJson)
      throws JsonProcessingException {
    return objectMapper.readValue(
        """
        {
          "id": "FOO1",
          "type": "FOO",
          "attributes": {
            "foo-rating": %s
          },
          "metadata": {
            "provenance": "TODO: ENTITYMETADATA"
          }
        }"""
            .formatted(rawAttributeJson)
            .stripIndent()
            .trim(),
        EntityResponse.class);
  }
}
