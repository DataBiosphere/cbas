package bio.terra.cbas.runsets.outputs;

import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StockOutputDefinitions {

  private StockOutputDefinitions() {}

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static WorkflowOutputDefinition primitiveOutputDefinition(
      String outputName, String outputType, String recordAttribute) throws Exception {
    String rawOutputDefinition =
        """
        {
          "output_name":"%s",
          "output_type": { "type": "primitive", "primitive_type": "%s" },
          "destination": { "type": "record_update", "record_attribute": "%s" }
        }
        """
            .formatted(outputName, outputType, recordAttribute)
            .stripIndent()
            .trim();

    return objectMapper.readValue(rawOutputDefinition, new TypeReference<>() {});
  }

  public static WorkflowOutputDefinition primitiveOutputDefinitionToNone(
      String outputName, String outputType) throws Exception {
    String rawOutputDefinition =
        """
        {
          "output_name":"%s",
          "output_type": { "type": "primitive", "primitive_type": "%s" },
          "destination": { "type": "none" }
        }
        """
            .formatted(outputName, outputType)
            .stripIndent()
            .trim();

    return objectMapper.readValue(rawOutputDefinition, new TypeReference<>() {});
  }

  public static WorkflowOutputDefinition optionalOutputDefinition(
      String outputName, String innerOutputType, String recordAttribute) throws Exception {
    String rawOutputDefinition =
        """
        {
          "output_name":"%s",
          "output_type": { "type": "optional", "optional_type": { "type": "primitive", "primitive_type": "%s" }},
          "destination": { "type": "record_update", "record_attribute": "%s" }
        }
        """
            .formatted(outputName, innerOutputType, recordAttribute)
            .stripIndent()
            .trim();

    return objectMapper.readValue(rawOutputDefinition, new TypeReference<>() {});
  }

  public static WorkflowOutputDefinition arrayOutputDefinition(
      String outputName, String innerOutputType, String recordAttributeName) throws Exception {
    String rawOutputDefinition =
        """
        {
          "output_name":"%s",
          "output_type": { "type": "array", "array_type": { "type": "primitive", "primitive_type": "%s" }},
          "destination": { "type": "record_update", "record_attribute": "%s" }
        }
        """
            .formatted(outputName, innerOutputType, recordAttributeName)
            .stripIndent()
            .trim();

    return objectMapper.readValue(rawOutputDefinition, new TypeReference<>() {});
  }
}
