package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.getOutputParameterType;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womtoolToCbasOutputs;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.exceptions.WomtoolValueTypeProcessingException.WomtoolValueTypeNotFoundException;
import bio.terra.cbas.model.OutputDestinationNone;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import com.google.gson.Gson;
import cromwell.client.model.ToolOutputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WomToolOutputsTest {

  List<ToolOutputParameter> womtoolOutputs;
  List<WorkflowOutputDefinition> cbasOutputDef;

  String womtoolStringOutput =
      """
    {
        "name": "hello",
        "valueType": {
          "typeName": "String"
        }
    }
    """;

  String womtoolIntOutput =
      """
    {
      "name": "hello",
      "valueType": {
        "typeName": "Int"
      }
    }
    """;

  String womtoolFloatOutput =
      """
    {
      "name": "hello",
      "valueType": {
        "typeName": "Float"
      }
    }
    """;

  String womtoolBooleanOutput =
      """
    {
      "name": "hello",
      "valueType": {
        "typeName": "Boolean"
      }
    }
    """;

  String womtoolFileOutput =
      """
    {
      "name": "hello",
      "valueType": {
        "typeName": "File"
      }
    }
    """;

  String womtoolArrayOutput =
      """
    {
      "name": "hello",
      "valueType": {
        "typeName": "Array",
        "arrayType": {
          "typeName": "String"
        },
        "nonEmpty": false
      }
    }
    """;

  String womtoolMapOutput =
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
        }
      }
      """;

  String womtoolOptionalOutput =
      """
      {
        "name": "hello",
        "valueType": {
          "typeName": "Optional",
          "optionalType": {
            "typeName": "File"
          }
        }
      }
      """;

  @BeforeEach
  public void init() {
    womtoolOutputs = new ArrayList<>();
    cbasOutputDef = new ArrayList<>();
  }

  @AfterEach
  public void afterEach() {
    womtoolOutputs.clear();
    cbasOutputDef.clear();
  }

  @Test
  void test_string() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolOutput =
        object.fromJson(womtoolStringOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolOutput);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .destination(new OutputDestinationNone());
    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  @Test
  void test_int() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolOutput =
        object.fromJson(womtoolIntOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolOutput);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.INT)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .destination(new OutputDestinationNone());
    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  @Test
  void test_float() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolOutput =
        object.fromJson(womtoolFloatOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolOutput);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.FLOAT)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .destination(new OutputDestinationNone());
    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  @Test
  void test_file() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolOutput =
        object.fromJson(womtoolFileOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolOutput);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.FILE)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .destination(new OutputDestinationNone());
    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  @Test
  void test_boolean() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolBool =
        object.fromJson(womtoolBooleanOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolBool);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.BOOLEAN)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .destination(new OutputDestinationNone());
    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  @Test
  void test_maps() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolMap = object.fromJson(womtoolMapOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolMap);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionMap()
                    .keyType(PrimitiveParameterValueType.STRING)
                    .valueType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.INT)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.MAP))
            .destination(new OutputDestinationNone());

    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  @Test
  void test_arrays() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolArray =
        object.fromJson(womtoolArrayOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolArray);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionArray()
                    .nonEmpty(false)
                    .arrayType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.STRING)
                            .type(ParameterTypeDefinition.TypeEnum.ARRAY))
                    .type(ParameterTypeDefinition.TypeEnum.ARRAY))
            .destination(new OutputDestinationNone());

    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  @Test
  void test_optional() throws WomtoolValueTypeNotFoundException {
    Gson object = new Gson();
    ToolOutputParameter womtoolOutput =
        object.fromJson(womtoolOptionalOutput, ToolOutputParameter.class);

    womtoolOutputs.add(womtoolOutput);
    WorkflowDescription workflowDescription = new WorkflowDescription().outputs(womtoolOutputs);

    WorkflowOutputDefinition cbasThing =
        new WorkflowOutputDefinition()
            .outputName("null.hello")
            .outputType(
                new ParameterTypeDefinitionOptional()
                    .optionalType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.FILE)
                            .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
                    .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
            .destination(new OutputDestinationNone());

    cbasOutputDef.add(cbasThing);

    assertEquals(cbasOutputDef, womtoolToCbasOutputs(workflowDescription));
  }

  /*
   * Testing the recursivelyGetParameterType() function
   */

  @Test
  void test_recursive_string() throws WomtoolValueTypeNotFoundException {

    String valueType =
        """
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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
  }

  @Test
  void test_recursive_file() throws WomtoolValueTypeNotFoundException {

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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
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

    assertEquals(cbasParameterTypeDef, getOutputParameterType(womtoolString));
  }
}
