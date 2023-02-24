package bio.terra.cbas.runsets.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.ParameterTypeDefinitionStruct;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.StructField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

  @Test
  void parseStructDefinition() throws Exception {
    String structDefinition =
        """
        {
          "type": "struct",
          "name": "Pet",
          "fields": [{
            "field_name": "name",
            "field_type": {
              "type": "primitive",
              "primitive_type": "String"
            }
          }, {
            "field_name": "species",
            "field_type": {
              "type": "struct",
              "name": "PetSpecies",
              "fields": [{
                "field_name": "species_name",
                "field_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                }
              }, {
                "field_name": "breed_name",
                "field_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                }
              }]
            }
          }]
        }""";

    ParameterTypeDefinition expectedType =
        new ParameterTypeDefinitionStruct()
            .name("Pet")
            .fields(
                List.of(
                    new StructField()
                        .fieldName("name")
                        .fieldType(
                            new ParameterTypeDefinitionPrimitive()
                                .primitiveType(PrimitiveParameterValueType.STRING)
                                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE)),
                    new StructField()
                        .fieldName("species")
                        .fieldType(
                            new ParameterTypeDefinitionStruct()
                                .name("PetSpecies")
                                .fields(
                                    List.of(
                                        new StructField()
                                            .fieldName("species_name")
                                            .fieldType(
                                                new ParameterTypeDefinitionPrimitive()
                                                    .primitiveType(
                                                        PrimitiveParameterValueType.STRING)
                                                    .type(
                                                        ParameterTypeDefinition.TypeEnum
                                                            .PRIMITIVE)),
                                        new StructField()
                                            .fieldName("breed_name")
                                            .fieldType(
                                                new ParameterTypeDefinitionPrimitive()
                                                    .primitiveType(
                                                        PrimitiveParameterValueType.STRING)
                                                    .type(
                                                        ParameterTypeDefinition.TypeEnum
                                                            .PRIMITIVE))))
                                .type(ParameterTypeDefinition.TypeEnum.STRUCT))))
            .type(ParameterTypeDefinition.TypeEnum.STRUCT);
    ParameterTypeDefinition actualType =
        objectMapper.readValue(structDefinition, new TypeReference<>() {});
    assertEquals(expectedType, actualType);
  }
}
