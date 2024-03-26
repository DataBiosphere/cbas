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
          "input_type": { "type": "map", "key_type": "%s", "value_type": { "type": "primitive", "primitive_type": "%s" }},
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

  public static WorkflowInputDefinition
      inputDefinitionWithOneFieldStructFooRatingParameterRecordLookup(
          String fieldName, String fieldType) throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": {
            "type": "struct",
            "name": "StructName",
            "fields": [{
              "field_name": "%s",
              "field_type": {
                "type": "primitive",
                "primitive_type": "%s"
              }
            }]
          },
          "source": {
            "type": "record_lookup",
            "record_attribute": "foo-rating"
          }
        }"""
            .formatted(fieldName, fieldType)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  private static String oneFieldStructInputDefinitionTemplate =
      """
        {
          "input_name": "lookup_foo",
          "input_type": {
            "type": "struct",
            "name": "StructName",
            "fields": [{
              "field_name": "%s",
              "field_type": {
                "type": "primitive",
                "primitive_type": "%s"
              }
            }]
          },
          "source": {
            "type": "object_builder",
            "fields": [{
              "name": "%s",
              "source": {
                "type": "record_lookup",
                "record_attribute": "foo-rating"
              }
            }]
          }
        }""";

  public static WorkflowInputDefinition inputDefinitionWithNestedOptionalStructInputsLiteral()
      throws JsonProcessingException {
    String paramDefinitionJson =
        nestedOptionalStructInsideOptionalStructLiteralJson.stripIndent().trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  private static String nestedOptionalStructInsideOptionalStructLiteralJson =
      """
       {
         "input_name": "literal_foo",
         "input_type": {
           "type": "optional",
           "optional_type": {
             "type": "struct",
             "name": "Struct",
             "fields": [
               {
                 "field_name": "foo",
                 "field_type": {
                   "type": "optional",
                   "optional_type": {
                     "type": "struct",
                     "name": "Struct",
                     "fields": [
                       {
                         "field_name": "x",
                         "field_type": {
                           "type": "primitive",
                           "primitive_type": "Int"
                         }
                       }
                     ]
                   }
                 }
               }
             ]
           }
         },
         "source": {
           "type": "object_builder",
           "fields": [
             {
               "name": "foo",
               "source": {
                 "type": "object_builder",
                 "fields": [
                   {
                     "name": "x",
                     "source": {
                       "type": "literal",
                       "parameter_value": 17
                     }
                   }
                 ]
               }
             }
           ]
         }
       }""";

  public static WorkflowInputDefinition inputDefinitionWithNestedOptionalStructInputsLookup()
      throws JsonProcessingException {
    String paramDefinitionJson =
        nestedOptionalStructInsideOptionalStructLookupJson.stripIndent().trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  private static String nestedOptionalStructInsideOptionalStructLookupJson =
      """
      {
         "input_name": "lookup_foo",
         "input_type": {
           "type": "optional",
           "optional_type": {
             "type": "struct",
             "name": "Struct",
             "fields": [
               {
                 "field_name": "foo",
                 "field_type": {
                   "type": "optional",
                   "optional_type": {
                     "type": "struct",
                     "name": "Struct",
                     "fields": [
                       {
                         "field_name": "x",
                         "field_type": {
                           "type": "primitive",
                           "primitive_type": "Int"
                         }
                       }
                     ]
                   }
                 }
               }
             ]
           }
         },
         "source": {
           "type": "object_builder",
           "fields": [
             {
               "name": "foo",
               "source": {
                 "type": "object_builder",
                 "fields": [
                   {
                     "name": "x",
                     "source": {
                       "type": "record_lookup",
                       "record_attribute": "foo-rating"
                     }
                   }
                 ]
               }
             }
           ]
         }
       }""";

  public static WorkflowInputDefinition inputDefinitionStructBuilderForOptionalInt()
      throws JsonProcessingException {
    String paramDefinitionJson = structBuilderForOptionalIntInput.stripIndent().trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  private static String structBuilderForOptionalIntInput =
      """
      {
         "input_name": "lookup_foo",
         "input_type": {
           "type": "optional",
           "optional_type": {
             "type": "primitive",
             "primitive_type": "Int"
           }
         },
         "source": {
           "type": "object_builder",
           "fields": [
             {
               "name": "foo",
               "source": {
                 "type": "object_builder",
                 "fields": [
                   {
                     "name": "x",
                     "source": {
                       "type": "record_lookup",
                       "record_attribute": "foo-rating"
                     }
                   }
                 ]
               }
             }
           ]
         }
       }""";

  public static WorkflowInputDefinition
      inputDefinitionWithOneFieldStructFooRatingParameterObjectBuilder(
          String fieldName, String fieldType) throws JsonProcessingException {
    String paramDefinitionJson =
        oneFieldStructInputDefinitionTemplate
            .formatted(fieldName, fieldType, fieldName)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  public static WorkflowInputDefinition nestedStructInputDefinitionWithBadFieldNamesInSource(
      String fieldName, String fieldType) throws JsonProcessingException {
    String paramDefinitionJson =
        oneFieldStructInputDefinitionTemplate
            .formatted(fieldName, fieldType, fieldName + "oops")
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  public static WorkflowInputDefinition
      inputDefinitionWithOneNestedFieldStructFooRatingParameterObjectBuilder(
          String fieldName, String innerFieldName, String innerFieldType)
          throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": {
            "type": "struct",
            "name": "StructName1",
            "fields": [{
              "field_name": "%s",
              "field_type": {
                "type": "struct",
                "name": "StructName2",
                "fields": [{
                  "field_name": "%s",
                  "field_type": {
                    "type": "primitive",
                    "primitive_type": "%s"
                  }
                }]
              }
            }]
          },
          "source": {
            "type": "object_builder",
            "fields": [{
              "name": "%s",
              "source": {
                "type": "object_builder",
                "fields": [{
                  "name": "%s",
                  "source": {
                    "type": "record_lookup",
                    "record_attribute": "foo-rating"
                  }
                }]
              }
            }]
          }
        }"""
            .formatted(fieldName, innerFieldName, innerFieldType, fieldName, innerFieldName)
            .stripIndent()
            .trim();

    return objectMapper.readValue(paramDefinitionJson, WorkflowInputDefinition.class);
  }

  public static WorkflowInputDefinition objectBuilderSourceUsedForStringInput()
      throws JsonProcessingException {
    String paramDefinitionJson =
        """
        {
          "input_name": "lookup_foo",
          "input_type": { "type": "primitive", "primitive_type": "string" },
          "source": {
            "type": "object_builder",
            "fields": [{
              "name": "struct_field",
              "source": {
                "type": "record_lookup",
                "record_attribute": "foo-rating"
              }
            }]
          }
        }"""
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
