package bio.terra.cbas.util.methods;

import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.getDestination;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.getParameterType;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.getSource;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womToCbasInputBuilder;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womToCbasOutputBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.cbas.common.exceptions.WomtoolValueTypeProcessingException.WomtoolValueTypeNotFoundException;
import bio.terra.cbas.model.MethodInputMapping;
import bio.terra.cbas.model.MethodOutputMapping;
import bio.terra.cbas.model.OutputDestination;
import bio.terra.cbas.model.OutputDestinationNone;
import bio.terra.cbas.model.OutputDestinationRecordUpdate;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionNone;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.ParameterTypeDefinitionStruct;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.StructField;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.ToolOutputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestWomtoolToCbasInputsAndOutputs {

  private static final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  List<ToolInputParameter> womtoolInputs;
  List<WorkflowInputDefinition> cbasInputDef;
  List<ToolOutputParameter> womtoolOutputs;
  List<WorkflowOutputDefinition> cbasOutputDef;

  ParameterDefinition recordLookUpParameterDefinition =
      new ParameterDefinitionRecordLookup()
          .recordAttribute("foo_rating")
          .type(ParameterDefinition.TypeEnum.RECORD_LOOKUP);
  ParameterDefinition literalValueParameterDefinition =
      new ParameterDefinitionLiteralValue()
          .parameterValue("bar_rating")
          .type(ParameterDefinition.TypeEnum.LITERAL);

  OutputDestination defaultDestinationDefinition =
      new OutputDestinationNone().type(OutputDestination.TypeEnum.NONE);
  OutputDestination recordUpdateDestinationDefinition =
      new OutputDestinationRecordUpdate()
          .recordAttribute("bar_record_update")
          .type(OutputDestination.TypeEnum.RECORD_UPDATE);

  @BeforeEach
  public void init() {
    womtoolInputs = new ArrayList<>();
    cbasInputDef = new ArrayList<>();
    womtoolOutputs = new ArrayList<>();
    cbasOutputDef = new ArrayList<>();
  }

  @AfterEach
  public void afterEach() {
    womtoolInputs.clear();
    cbasInputDef.clear();
    womtoolOutputs.clear();
    cbasOutputDef.clear();
  }

  /*
   * Testing the womToCbasInputBuilder() function
   * */
  @Test
  void test_multiple_inputs() throws WomtoolValueTypeNotFoundException {
    String womtoolStringInputs =
        """
            {
              "valid": true,
              "errors": [],
              "validWorkflow": true,
              "name": "test",
              "inputs": [
                {
                  "name": "hello",
                  "valueType": {
                    "typeName": "String"
                  },
                  "typeDisplayName": "String",
                  "optional": false,
                  "default": "Workflow Management"
                },
                {
                  "name": "foo",
                  "valueType": {
                    "typeName": "String"
                  },
                  "typeDisplayName": "String",
                  "optional": true,
                  "default": null
                },
                {
                  "name": "bar",
                  "valueType": {
                    "typeName": "Optional",
                    "optionalType": {
                      "typeName": "Int"
                    }
                  },
                  "typeDisplayName": "Int?",
                  "optional": true,
                  "default": null
                }
              ],
              "outputs": []
             }
            """;

    Gson object = new Gson();
    WorkflowDescription womtoolDescription =
        object.fromJson(womtoolStringInputs, WorkflowDescription.class);

    WorkflowInputDefinition input1 =
        new WorkflowInputDefinition()
            .inputName("test.hello")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(new ParameterDefinitionNone().type(ParameterDefinition.TypeEnum.NONE));
    WorkflowInputDefinition input2 =
        new WorkflowInputDefinition()
            .inputName("test.foo")
            .inputType(
                new ParameterTypeDefinitionOptional()
                    .optionalType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.STRING)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
            .source(new ParameterDefinitionNone().type(ParameterDefinition.TypeEnum.NONE));
    WorkflowInputDefinition input3 =
        new WorkflowInputDefinition()
            .inputName("test.bar")
            .inputType(
                new ParameterTypeDefinitionOptional()
                    .optionalType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.INT)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
            .source(new ParameterDefinitionNone().type(ParameterDefinition.TypeEnum.NONE));

    cbasInputDef.add(input1);
    cbasInputDef.add(input2);
    cbasInputDef.add(input3);

    assertEquals(cbasInputDef, womToCbasInputBuilder(womtoolDescription, new ArrayList<>()));
  }

  @Test
  void womToCbasInputBuilderWithInputMappings() throws Exception {
    String workflowDescriptionAsString =
        """
        {
          "valid": true,
          "errors": [],
          "validWorkflow": true,
          "name": "hello_world",
          "inputs": [
            {
              "name": "foo",
              "valueType": {
                "typeName": "STRING"
              },
              "typeDisplayName": "String",
              "optional": true,
              "default": "Hello World"
            },
            {
              "name": "bar",
              "valueType": {
                "typeName": "STRING"
              },
              "typeDisplayName": "String",
              "optional": true,
              "default": null
            }
          ],
          "outputs": []
         }
        """;
    String validInputMappingString =
        """
        [
          {
            "input_name": "hello_world.foo",
            "source": {
              "type": "record_lookup",
              "record_attribute": "foo_rating"
            }
          }
        ]
        """;

    WorkflowDescription workflowDescription =
        objectMapper.readValue(workflowDescriptionAsString, new TypeReference<>() {});
    List<MethodInputMapping> methodInputMappings =
        objectMapper.readValue(validInputMappingString, new TypeReference<>() {});

    WorkflowInputDefinition input1 =
        new WorkflowInputDefinition()
            .inputName("hello_world.foo")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(recordLookUpParameterDefinition);
    WorkflowInputDefinition input2 =
        new WorkflowInputDefinition()
            .inputName("hello_world.bar")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.NONE));

    cbasInputDef.add(input1);
    cbasInputDef.add(input2);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription, methodInputMappings));
  }

  @Test
  void fail_unhandled_types() {
    String valueType1 = """
          {
            "typeName": "Pair"
          }
        """;

    Gson object = new Gson();
    ValueType womtoolString1 = object.fromJson(valueType1, ValueType.class);

    assertThrows(WomtoolValueTypeNotFoundException.class, () -> getParameterType(womtoolString1));
  }

  @Test
  void test_multiple_outputs() throws WomtoolValueTypeNotFoundException {
    String womtoolWorkflowDescription =
        """
        {
          "valid": true,
          "errors": [],
          "validWorkflow": true,
          "name": "test",
          "inputs": [],
          "outputs": [
          {
                "name": "foo",
                "valueType": {
                  "typeName": "String"
                },
                "typeDisplayName": "String"
              },
              {
                "name": "bar",
                "valueType": {
                  "typeName": "Array",
                  "arrayType": {
                    "typeName": "String"
                  }
                },
                "typeDisplayName": "Array[String]"
              }]
         }
        """;

    Gson object = new Gson();
    WorkflowDescription womtoolDescription =
        object.fromJson(womtoolWorkflowDescription, WorkflowDescription.class);

    WorkflowOutputDefinition output1 =
        new WorkflowOutputDefinition()
            .outputName("test.foo")
            .outputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .destination(new OutputDestinationNone().type(OutputDestination.TypeEnum.NONE));
    WorkflowOutputDefinition output2 =
        new WorkflowOutputDefinition()
            .outputName("test.bar")
            .outputType(
                new ParameterTypeDefinitionArray()
                    .arrayType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.STRING)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .nonEmpty(null)
                    .type(ParameterTypeDefinition.TypeEnum.ARRAY))
            .destination(new OutputDestinationNone().type(OutputDestination.TypeEnum.NONE));
    cbasOutputDef.add(output1);
    cbasOutputDef.add(output2);

    assertEquals(cbasOutputDef, womToCbasOutputBuilder(womtoolDescription, new ArrayList<>()));
  }

  @Test
  void womToCbasInputBuilderWithOutputMappings() throws Exception {
    String workflowDescriptionAsString =
        """
        {
          "valid": true,
          "errors": [],
          "validWorkflow": true,
          "name": "hello_world",
          "inputs": [],
          "outputs": [
          {
                "name": "foo",
                "valueType": {
                  "typeName": "STRING"
                },
                "typeDisplayName": "String"
              },
              {
                "name": "bar",
                "valueType": {
                  "typeName": "ARRAY",
                  "arrayType": {
                    "typeName": "STRING"
                  }
                },
                "typeDisplayName": "Array[String]"
              }]
         }
        """;
    String validOutputMappingString =
        """
        [
          {
            "output_name": "hello_world.bar",
            "destination": {
              "type": "record_update",
              "record_attribute": "bar_record_update"
            }
          }
        ]
        """;

    WorkflowDescription workflowDescription =
        objectMapper.readValue(workflowDescriptionAsString, new TypeReference<>() {});
    List<MethodOutputMapping> methodOutputMappings =
        objectMapper.readValue(validOutputMappingString, new TypeReference<>() {});

    WorkflowOutputDefinition output1 =
        new WorkflowOutputDefinition()
            .outputName("hello_world.foo")
            .outputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .destination(new OutputDestinationNone().type(OutputDestination.TypeEnum.NONE));
    WorkflowOutputDefinition output2 =
        new WorkflowOutputDefinition()
            .outputName("hello_world.bar")
            .outputType(
                new ParameterTypeDefinitionArray()
                    .arrayType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.STRING)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .nonEmpty(null)
                    .type(ParameterTypeDefinition.TypeEnum.ARRAY))
            .destination(recordUpdateDestinationDefinition);
    cbasOutputDef.add(output1);
    cbasOutputDef.add(output2);

    assertEquals(cbasOutputDef, womToCbasOutputBuilder(workflowDescription, methodOutputMappings));
  }

  /*
   * Testing the getParameterType() function
   */

  @Test
  void test_string_type() throws WomtoolValueTypeNotFoundException {

    String valueType = """
          {
            "typeName": "String"
          }
          """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionPrimitive cbasParameterTypeDef = new ParameterTypeDefinitionPrimitive();

    cbasParameterTypeDef
        .primitiveType(PrimitiveParameterValueType.STRING)
        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_int_type() throws WomtoolValueTypeNotFoundException {

    String valueType = """
          {
            "typeName": "Int"
          }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionPrimitive cbasParameterTypeDef = new ParameterTypeDefinitionPrimitive();

    cbasParameterTypeDef
        .primitiveType(PrimitiveParameterValueType.INT)
        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_float_type() throws WomtoolValueTypeNotFoundException {

    String valueType = """
        {
          "typeName": "Float"
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionPrimitive cbasParameterTypeDef = new ParameterTypeDefinitionPrimitive();

    cbasParameterTypeDef
        .primitiveType(PrimitiveParameterValueType.FLOAT)
        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_boolean_type() throws WomtoolValueTypeNotFoundException {

    String valueType = """
        {
          "typeName": "Boolean"
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionPrimitive cbasParameterTypeDef = new ParameterTypeDefinitionPrimitive();

    cbasParameterTypeDef
        .primitiveType(PrimitiveParameterValueType.BOOLEAN)
        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_file_type() throws WomtoolValueTypeNotFoundException {

    String valueType = """
        {
          "typeName": "File"
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionPrimitive cbasParameterTypeDef = new ParameterTypeDefinitionPrimitive();

    cbasParameterTypeDef
        .primitiveType(PrimitiveParameterValueType.FILE)
        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_array_type() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
        {
           "typeName": "Array",
           "arrayType": {
             "typeName": "File"
           },
           "nonEmpty": true
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionArray cbasParameterTypeDef = new ParameterTypeDefinitionArray();

    cbasParameterTypeDef
        .nonEmpty(true)
        .arrayType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.FILE)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
        .type(ParameterTypeDefinition.TypeEnum.ARRAY);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_map_type() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
        {
           "typeName": "Map",
           "mapType": {
             "keyType": {
               "typeName": "String"
             },
             "valueType": {
               "typeName": "Int"
             }
           }
         }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionMap cbasParameterTypeDef = new ParameterTypeDefinitionMap();

    cbasParameterTypeDef
        .valueType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.INT)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
        .keyType(PrimitiveParameterValueType.STRING)
        .type(ParameterTypeDefinition.TypeEnum.MAP);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_optional_type() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
        {
           "typeName": "Optional",
           "optionalType": {
             "typeName": "String"
           }
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionOptional cbasParameterTypeDef = new ParameterTypeDefinitionOptional();

    cbasParameterTypeDef
        .optionalType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.STRING)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
        .type(ParameterTypeDefinition.TypeEnum.OPTIONAL);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_array_nested_in_map() throws WomtoolValueTypeNotFoundException {
    String mapNestedInArrayType =
        """
        {
          "typeName": "Map",
          "mapType": {
            "keyType": {
              "typeName": "String"
            },
            "valueType": {
              "typeName": "Array",
                 "arrayType": {
                   "typeName": "Int"
                 },
                 "nonEmpty": false
            }
          }
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(mapNestedInArrayType, ValueType.class);

    ParameterTypeDefinitionMap cbasParameterTypeDef = new ParameterTypeDefinitionMap();

    cbasParameterTypeDef
        .keyType(PrimitiveParameterValueType.STRING)
        .valueType(
            new ParameterTypeDefinitionArray()
                .nonEmpty(false)
                .arrayType(
                    new ParameterTypeDefinitionPrimitive()
                        .primitiveType(PrimitiveParameterValueType.INT)
                        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                .type(ParameterTypeDefinition.TypeEnum.ARRAY))
        .type(ParameterTypeDefinition.TypeEnum.MAP);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_nested_arrays() throws WomtoolValueTypeNotFoundException {
    String arrayNestedInArrayType =
        """
        {
            "typeName": "Array",
            "arrayType": {
              "typeName": "Array",
              "arrayType": {
                "typeName": "String"
              },
              "nonEmpty": false
            },
            "nonEmpty": false
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(arrayNestedInArrayType, ValueType.class);

    ParameterTypeDefinitionArray cbasParameterTypeDef = new ParameterTypeDefinitionArray();

    cbasParameterTypeDef
        .nonEmpty(false)
        .arrayType(
            new ParameterTypeDefinitionArray()
                .nonEmpty(false)
                .arrayType(
                    new ParameterTypeDefinitionPrimitive()
                        .primitiveType(PrimitiveParameterValueType.STRING)
                        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                .type(ParameterTypeDefinition.TypeEnum.ARRAY))
        .type(ParameterTypeDefinition.TypeEnum.ARRAY);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_map_nested_in_array() throws WomtoolValueTypeNotFoundException {
    String mapNestedInArrayType =
        """
        {
            "typeName": "Array",
            "arrayType": {
              "typeName": "Map",
              "mapType": {
                "keyType": {
                  "typeName": "String"
                },
                "valueType": {
                  "typeName": "String"
                }
              }
            },
            "nonEmpty": false
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(mapNestedInArrayType, ValueType.class);

    ParameterTypeDefinitionArray cbasParameterTypeDef = new ParameterTypeDefinitionArray();

    cbasParameterTypeDef
        .nonEmpty(false)
        .arrayType(
            new ParameterTypeDefinitionMap()
                .keyType(PrimitiveParameterValueType.STRING)
                .valueType(
                    new ParameterTypeDefinitionPrimitive()
                        .primitiveType(PrimitiveParameterValueType.STRING)
                        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                .type(ParameterTypeDefinition.TypeEnum.MAP))
        .type(ParameterTypeDefinition.TypeEnum.ARRAY);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_struct_type() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
        {
           "typeName": "Object",
           "objectFieldTypes": [
             {
               "fieldName": "foo",
               "fieldType": {
                 "typeName": "Int"
               }
             },
             {
               "fieldName": "bar",
               "fieldType": {
                 "typeName": "Int"
               }
             }
           ]
        }
        """;

    Gson object = new Gson();
    ValueType womtoolString = object.fromJson(valueType, ValueType.class);

    ParameterTypeDefinitionStruct cbasParameterTypeDef = new ParameterTypeDefinitionStruct();

    List<StructField> field =
        new ArrayList<>(
            Arrays.asList(
                new StructField()
                    .fieldName("foo")
                    .fieldType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.INT)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE)),
                new StructField()
                    .fieldName("bar")
                    .fieldType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.INT)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))));

    cbasParameterTypeDef.name("Struct").fields(field).type(ParameterTypeDefinition.TypeEnum.STRUCT);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void getSourceForInputWithMappingInRequest() {
    Map<String, ParameterDefinition> methodInputMappingAsMap = new HashMap<>();
    methodInputMappingAsMap.put("hello_world.input_name_1", recordLookUpParameterDefinition);
    methodInputMappingAsMap.put("hello_world.input_name_2", literalValueParameterDefinition);

    assertEquals(
        recordLookUpParameterDefinition,
        getSource("hello_world.input_name_1", "default_value", methodInputMappingAsMap));
  }

  @Test
  void getSourceForInputWithoutMappingInRequest() {
    Map<String, ParameterDefinition> methodInputMappingAsMap = new HashMap<>();
    methodInputMappingAsMap.put("hello_world.input_name_1", recordLookUpParameterDefinition);

    ParameterDefinition defaultSource =
        new ParameterDefinitionLiteralValue()
            .parameterValue("default_value")
            .type(ParameterDefinition.TypeEnum.NONE);

    assertEquals(
        defaultSource,
        getSource("hello_world.input_name_2", "default_value", methodInputMappingAsMap));
  }

  @Test
  void getDestinationForOutputWithMappingInRequest() {
    Map<String, OutputDestination> methodOutputMappingAsMap = new HashMap<>();
    methodOutputMappingAsMap.put("hello_world.output_name_1", recordUpdateDestinationDefinition);

    assertEquals(
        recordUpdateDestinationDefinition,
        getDestination("hello_world.output_name_1", methodOutputMappingAsMap));
  }

  @Test
  void getDestinationForOutputWithoutMappingInRequest() {
    Map<String, OutputDestination> methodOutputMappingAsMap = new HashMap<>();

    assertEquals(
        defaultDestinationDefinition,
        getDestination("hello_world.output_name_1", methodOutputMappingAsMap));
  }

  //  @Test
  //  void getInputTypeForInputWithOptionalTrue() {
  //
  //  }
}
