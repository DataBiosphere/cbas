package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.InputGenerator.womToCbasInputBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionNone;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowInputDefinition;
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WomToolInputsTest {

  List<ToolInputParameter> womtoolInputs;
  List<WorkflowInputDefinition> cbasInputDef;

  @BeforeEach
  public void init() {
    womtoolInputs = new ArrayList<>();
    cbasInputDef = new ArrayList<>();
  }

  @AfterEach
  public void afterEach() {}

  @Test
  void test_string() {
    ToolInputParameter womToolThing =
        new ToolInputParameter()
            .name("hello")
            .valueType(new ValueType().typeName(ValueType.TypeNameEnum.STRING))
            ._default("test");
    womtoolInputs.add(womToolThing);
    WorkflowDescription workflowDescription = new WorkflowDescription().inputs(womtoolInputs);

    WorkflowInputDefinition cbasThing =
        new WorkflowInputDefinition()
            .inputName("null.hello")
            .inputType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .source(new ParameterDefinitionLiteralValue());
    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_ints() {
    ToolInputParameter womToolThing =
        new ToolInputParameter()
            .name("hello")
            .valueType(new ValueType().typeName(ValueType.TypeNameEnum.INT));
    womtoolInputs.add(womToolThing);
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
    ToolInputParameter womToolThing =
        new ToolInputParameter()
            .name("hello")
            .valueType(new ValueType().typeName(ValueType.TypeNameEnum.FLOAT));
    womtoolInputs.add(womToolThing);
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
    ToolInputParameter womToolThing =
        new ToolInputParameter()
            .name("hello")
            .valueType(new ValueType().typeName(ValueType.TypeNameEnum.BOOLEAN))
            ._default(null);
    womtoolInputs.add(womToolThing);
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

  //  @Test
  //  void test_maps() {
  //    ToolInputParameter womToolThing =
  //        new ToolInputParameter()
  //            .name("hello")
  //            .valueType(new ValueType().typeName(ValueType.TypeNameEnum.MAP));
  //    WorkflowInputDefinition cbasThing =
  //        new WorkflowInputDefinition()
  //            .inputName("hello")
  //            .inputType(new
  // ParameterTypeDefinitionMap().type(ParameterTypeDefinition.TypeEnum.MAP));
  //
  //    assertEquals(cbasThing, womToolInputToCbasInput(womToolThing));
  //  }
  //
  @Test
  void test_arrays() {
    ToolInputParameter womToolThing =
        new ToolInputParameter()
            .name("hello")
            .valueType(new ValueType().typeName(ValueType.TypeNameEnum.ARRAY))
            ._default(null);
    womtoolInputs.add(womToolThing);
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
                                    String.valueOf(womToolThing.getValueType())))
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.ARRAY))
            .source(new ParameterDefinitionNone());

    cbasInputDef.add(cbasThing);

    assertEquals(cbasInputDef, womToCbasInputBuilder(workflowDescription));
  }

  @Test
  void test_optional() {
    ToolInputParameter womToolThing =
        new ToolInputParameter()
            .name("hello")
            .valueType(new ValueType().typeName(ValueType.TypeNameEnum.OPTIONAL))
            ._default(null);
    womtoolInputs.add(womToolThing);
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
