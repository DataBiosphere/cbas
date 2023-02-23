package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.InputGenerator.womToCbasInputBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.ParameterDefinitionNone;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionMapMapType;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowInputDefinition;
import com.google.gson.Gson;
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WomToolInputsTest {

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
  public void afterEach() {}

  @Test
  void test_string() {
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
            .source(new ParameterDefinitionNone());
    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_ints() {
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
            .source(new ParameterDefinitionNone());
    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_floats() {
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
            .source(new ParameterDefinitionNone());
    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_bools() {
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
            .source(new ParameterDefinitionNone());

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_files() {
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
            .source(new ParameterDefinitionNone());

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_maps() {
    Gson object = new Gson();
    ToolInputParameter womtoolMap = object.fromJson(womtoolMapInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolMap);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionMap()
                    .mapType(
                        new ParameterTypeDefinitionMapMapType()
                            .keyType(PrimitiveParameterValueType.STRING)
                            .valueType(
                                new ParameterTypeDefinition()
                                    .type(ParameterTypeDefinition.TypeEnum.fromValue("Int"))))
                    .type(ParameterTypeDefinition.TypeEnum.MAP))
            .source(new ParameterDefinitionNone());

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_arrays() {
    Gson object = new Gson();
    ToolInputParameter womtoolArray = object.fromJson(womtoolArrayInput, ToolInputParameter.class);

    womtoolInputs.add(womtoolArray);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionArray()
                    .arrayType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(
                                PrimitiveParameterValueType.fromValue(
                                    String.valueOf(womtoolArray.getValueType())))
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.ARRAY))
            .source(new ParameterDefinitionNone());

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_optional() {
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
                        new ParameterTypeDefinitionOptional()
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
            .source(new ParameterDefinitionNone());

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }
}
