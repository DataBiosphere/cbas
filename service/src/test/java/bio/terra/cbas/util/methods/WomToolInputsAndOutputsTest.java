package bio.terra.cbas.util.methods;

import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.getParameterType;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womToCbasInputBuilder;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womToCbasOutputBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.cbas.common.exceptions.WomtoolValueTypeProcessingException.WomtoolValueTypeNotFoundException;
import bio.terra.cbas.model.OutputDestination;
import bio.terra.cbas.model.OutputDestinationNone;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
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
import com.google.gson.Gson;
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.ToolOutputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WomToolInputsTest {

  List<ToolInputParameter> womtoolInputs;
  List<WorkflowInputDefinition> cbasInputDef;
  List<ToolOutputParameter> womtoolOutputs;
  List<WorkflowOutputDefinition> cbasOutputDef;

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
  void test_multiple_string_inputs() throws WomtoolValueTypeNotFoundException {
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
              "optional": true,
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
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue("Workflow Management")
                    .type(ParameterDefinition.TypeEnum.NONE));
    WorkflowInputDefinition input2 =
        new WorkflowInputDefinition()
            .inputName("test.foo")
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

    assertEquals(cbasInputDef, womToCbasInputBuilder(womtoolDescription));
  }

  @Test
  void fail_unhandled_types() {
    String valueType1 = """
          {
            "typeName": "Pair"
          }
        """;
    String valueType2 = """
          {
            "typeName": "Pair"
          }
        """;

    Gson object = new Gson();
    ValueType womtoolString1 = object.fromJson(valueType1, ValueType.class);
    ValueType womtoolString2 = object.fromJson(valueType2, ValueType.class);

    assertThrows(WomtoolValueTypeNotFoundException.class, () -> getParameterType(womtoolString1));

    assertThrows(WomtoolValueTypeNotFoundException.class, () -> getParameterType(womtoolString2));
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

    assertEquals(cbasOutputDef, womToCbasOutputBuilder(womtoolDescription));
  }

  @Test
  void test_struct_type() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
        {
          "valid": true,
          "errors": [],
          "validWorkflow": true,
          "name": "Sample",
          "inputs": [
            {
               "name": "settings",
               "valueType": {
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
               },
               "typeDisplayName": "StructName",
               "optional": false,
               "default": null
            }
          ]
        }
        """;

    Gson object = new Gson();
    WorkflowDescription womtoolString = object.fromJson(valueType, WorkflowDescription.class);

    List<WorkflowInputDefinition> cbasParameterTypeDef = new ArrayList<>();
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

    cbasParameterTypeDef.add(
        new WorkflowInputDefinition()
            .inputName("Sample.settings")
            .inputType(
                new ParameterTypeDefinitionStruct()
                    .name("StructName")
                    .fields(field)
                    .type(ParameterTypeDefinition.TypeEnum.STRUCT))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.NONE)));
    assertEquals(cbasParameterTypeDef, womToCbasInputBuilder(womtoolString));
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
                .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
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
}
