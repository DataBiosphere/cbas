package bio.terra.cbas.runsets.inputs;

import bio.terra.cbas.model.WorkflowInputDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StockInputDefinitions {

  private StockInputDefinitions() {}

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static WorkflowInputDefinition fooRatingLiteralParameter(
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

  public static WorkflowInputDefinition fooRatingRecordLookupParameter(String parameterType)
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

  public static WorkflowInputDefinition inputDefinitionWithOptionalFooRatingParameter(
      String parameterType) throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": { "type": "optional", "optional_type": { "type": "primitive", "primitive_type": "%s" }},
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

  public static WorkflowInputDefinition inputDefinitionWithOptionalNoneParameter(
      String parameterType) throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": { "type": "optional", "optional_type": { "type": "primitive", "primitive_type": "%s" }},
          "source": {
            "type": "none"
          }
        }"""
            .formatted(parameterType)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  public static WorkflowInputDefinition inputDefinitionWithArrayFooRatingParameter(
      Boolean nonEmpty, String arrayInnerType) throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": { "type": "array", "non_empty": %s, "array_type": { "type": "primitive", "primitive_type": "%s" }},
          "source": {
            "type": "record_lookup",
            "record_attribute": "foo-rating"
          }
        }"""
            .formatted(nonEmpty, arrayInnerType)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  public static WorkflowInputDefinition inputDefinitionWithMapFooRatingParameter(
      String mapKeyType, String mapValueType) throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": { "type": "map", "key_type": { "type": "primitive", "primitive_type": "%s" }, "value_type": { "type": "primitive", "primitive_type": "%s" }},
          "source": {
            "type": "record_lookup",
            "record_attribute": "foo-rating"
          }
        }"""
            .formatted(mapKeyType, mapValueType)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  public static WorkflowInputDefinition inputDefinitionWithArrayLiteral(
      Boolean nonEmpty, String arrayInnerType, String rawLiteralJson)
      throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": { "type": "array", "non_empty": %s, "array_type": { "type": "primitive", "primitive_type": "%s" }},
          "source": {
            "type": "literal",
            "parameter_value": %s
          }
        }"""
            .formatted(nonEmpty, arrayInnerType, rawLiteralJson)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }
}
