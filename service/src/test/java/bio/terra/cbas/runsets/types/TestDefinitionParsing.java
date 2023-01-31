package bio.terra.cbas.runsets.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TestDefinitionParsing {

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void parseMapDefinition() throws Exception {
    String mapDefinition =
        """
        {
         "type": "map",
          "key_type": "String",
          "value_type": {
            "type": "primitive",
            "primitive_type": "String"
          }
        }""";

    ParameterTypeDefinition expectedType =
        new ParameterTypeDefinitionMap()
            .keyType(PrimitiveParameterValueType.STRING)
            .valueType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .type(ParameterTypeDefinition.TypeEnum.MAP);
    ParameterTypeDefinition actualType =
        objectMapper.readValue(mapDefinition, new TypeReference<>() {});
    assertEquals(expectedType, actualType);
  }
}
