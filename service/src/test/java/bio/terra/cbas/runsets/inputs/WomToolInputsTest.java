package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.getParameterType;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womToCbasInputBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.exceptions.WomtoolValueTypeProcessingException.WomtoolValueTypeNotFoundException;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowInputDefinition;
import com.google.gson.Gson;
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WomToolInputsTest {

  List<ToolInputParameter> womtoolInputs;
  List<WorkflowInputDefinition> cbasInputDef;

  String womtoolStringInput =
      """
      {
            "name": "hello",
            "valueType": {
              "typeName": "String"
            },
            "typeDisplayName": "String"
          }
      """;

  String womtoolOptionalInput =
      """
      {
            "name": "hello",
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
      """;

  String womtoolIntInput =
      """
      {
            "name": "hello",
            "valueType": {
              "typeName": "Int"
            },
            "typeDisplayName": "Int"
          }
      """;

  String womtoolFloatInput =
      """
      {
            "name": "hello",
            "valueType": {
              "typeName": "Float"
            },
            "typeDisplayName": "Float"
          }
      """;

  String womtoolBooleanInput =
      """
      {
        "name": "hello",
        "valueType": {
          "typeName": "Boolean"
        },
        "typeDisplayName": "Boolean"
      }
      """;

  String womtoolFileInput =
      """
        {
          "name": "hello",
          "valueType": {
            "typeName": "File"
          },
          "typeDisplayName": "File"
        }
      """;

  String womtoolArrayInput =
      """
      {
         "name": "hello",
         "valueType": {
           "typeName": "Array",
           "arrayType": {
             "typeName": "Array",
             "arrayType": {
               "typeName": "String"
             },
             "nonEmpty": false
           },
           "nonEmpty": false
         },
         "typeDisplayName": "Array[Array[String]]"
       }
      """;

  String womtoolMapInput =
      """
      {
               "name": "hello",
               "valueType": {
                 "typeName": "Map",
                 "mapType": {
                   "keyType": {
                     "typeName": "String"
                   },
                   "valueType": {
                     "typeName": "Int"
                   }
                 }
               },
               "typeDisplayName": "Map[String, Int]"
             }
      """;

  @BeforeEach
  public void init() {
    womtoolInputs = new ArrayList<>();
    cbasInputDef = new ArrayList<>();
  }

  @AfterEach
  public void afterEach() {
    womtoolInputs.clear();
    cbasInputDef.clear();
  }

  /*
   * Testing the womToCbasInputBuilder() function
   * */
  @Test
  void test_string() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolInput = object.fromJson(womtoolStringInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolInput);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));
    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_ints() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolInt = object.fromJson(womtoolIntInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolInt);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.INT)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));
    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_floats() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolFloat = object.fromJson(womtoolFloatInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolFloat);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.FLOAT)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));
    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_bools() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolBoolean =
        object.fromJson(womtoolBooleanInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolBoolean);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.BOOLEAN)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_files() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolFile = object.fromJson(womtoolFileInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolFile);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.FILE)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_maps() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolMap = object.fromJson(womtoolMapInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolMap);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionMap()
                    .keyType(PrimitiveParameterValueType.STRING)
                    .valueType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.INT)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.MAP))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_arrays() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolArray = object.fromJson(womtoolArrayInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolArray);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasDefinition =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionArray()
                    .nonEmpty(false)
                    .arrayType(
                        new ParameterTypeDefinitionArray()
                            .nonEmpty(false)
                            .arrayType(
                                new ParameterTypeDefinitionPrimitive()
                                    .primitiveType(PrimitiveParameterValueType.STRING)
                                    .type(ParameterTypeDefinition.TypeEnum.ARRAY))
                            .type(ParameterTypeDefinition.TypeEnum.ARRAY))
                    .type(ParameterTypeDefinition.TypeEnum.ARRAY))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));

    cbasInputDef.add(cbasDefinition);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_optional() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolInputParameter womtoolOptional =
        object.fromJson(womtoolOptionalInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolOptional);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionOptional()
                    .optionalType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.INT)
                            .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
                    .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
            .source(
                new ParameterDefinitionLiteralValue()
                    .parameterValue(null)
                    .type(ParameterDefinition.TypeEnum.LITERAL));

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  /*
   * Testing the recursivelyGetParameterType() function
   */

  @Test
  void test_recursive_string() throws WomtoolValueTypeNotFoundException {

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
  void test_recursive_int() throws WomtoolValueTypeNotFoundException {

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
  void test_recursive_float() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
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
  void test_recursive_boolean() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
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
  void test_recursive_file() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
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
  void test_recursive_array() throws WomtoolValueTypeNotFoundException {

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
                .type(ParameterTypeDefinition.TypeEnum.ARRAY))
        .type(ParameterTypeDefinition.TypeEnum.ARRAY);

    assertEquals(cbasParameterTypeDef, getParameterType(womtoolString));
  }

  @Test
  void test_recursive_map() throws WomtoolValueTypeNotFoundException {

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
  void test_recursive_optional() throws WomtoolValueTypeNotFoundException {

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
}
