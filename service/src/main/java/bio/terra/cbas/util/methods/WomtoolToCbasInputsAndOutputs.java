package bio.terra.cbas.util.methods;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.model.OutputDestinationNone;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.ToolOutputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WomtoolToCbasInputsAndOutputs {

  // Inputs
  public static ParameterTypeDefinition getParameterType(ValueType valueType)
      throws InputProcessingException.WomtoolInputTypeNotFoundException {

    if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.STRING)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.STRING)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.INT)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.INT)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.BOOLEAN)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.BOOLEAN)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.OPTIONAL)) {
      return new ParameterTypeDefinitionOptional()
          .optionalType(
              getParameterType(Objects.requireNonNull(valueType.getOptionalType()))
                  .type(ParameterTypeDefinition.TypeEnum.OPTIONAL));
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.FILE)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.FILE)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.ARRAY)) {
      return new ParameterTypeDefinitionArray()
          .nonEmpty(valueType.getNonEmpty())
          .arrayType(
              getParameterType(Objects.requireNonNull(valueType.getArrayType()))
                  .type(ParameterTypeDefinition.TypeEnum.ARRAY));
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.FLOAT)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.FLOAT)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.MAP)) {
      return new ParameterTypeDefinitionMap()
          .keyType(
              PrimitiveParameterValueType.fromValue(
                  Objects.requireNonNull(
                          Objects.requireNonNull(valueType.getMapType()).getKeyType().getTypeName())
                      .toString()))
          .valueType(
              getParameterType(Objects.requireNonNull(valueType.getMapType()).getValueType()))
          .type(ParameterTypeDefinition.TypeEnum.MAP);
    } else {
      throw new InputProcessingException.WomtoolInputTypeNotFoundException(valueType);
    }
  }

  public static List<WorkflowInputDefinition> womToCbasInputBuilder(WorkflowDescription womInputs)
      throws InputProcessingException.WomtoolInputTypeNotFoundException {
    List<WorkflowInputDefinition> cbasInputDefinition = new ArrayList<>();
    String workflowName = womInputs.getName();

    for (ToolInputParameter input : womInputs.getInputs()) {
      WorkflowInputDefinition workflowInputDefinition = new WorkflowInputDefinition();

      // Name
      workflowInputDefinition.inputName("%s.%s".formatted(workflowName, input.getName()));

      // Input type
      workflowInputDefinition.inputType(getParameterType(input.getValueType()));

      // Source
      workflowInputDefinition.source(
          new ParameterDefinitionLiteralValue()
              .parameterValue(input.getDefault())
              .type(ParameterDefinition.TypeEnum.LITERAL));

      cbasInputDefinition.add(workflowInputDefinition);
    }

    return cbasInputDefinition;
  }

  // Outputs
  public static ParameterTypeDefinition getOutputParameterType(ValueType valueType)
      throws OutputProcessingException.WomtoolOutputTypeNotFoundException {
    if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.STRING)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.STRING)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.INT)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.INT)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.BOOLEAN)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.BOOLEAN)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.OPTIONAL)) {
      return new ParameterTypeDefinitionOptional()
          .optionalType(
              getOutputParameterType(Objects.requireNonNull(valueType.getOptionalType()))
                  .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
          .type(ParameterTypeDefinition.TypeEnum.OPTIONAL);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.FILE)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.FILE)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.ARRAY)) {
      return new ParameterTypeDefinitionArray()
          .nonEmpty(valueType.getNonEmpty())
          .arrayType(
              getOutputParameterType(Objects.requireNonNull(valueType.getArrayType()))
                  .type(ParameterTypeDefinition.TypeEnum.ARRAY))
          .type(ParameterTypeDefinition.TypeEnum.ARRAY);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.FLOAT)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.FLOAT)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.MAP)) {
      return new ParameterTypeDefinitionMap()
          .keyType(
              PrimitiveParameterValueType.fromValue(
                  Objects.requireNonNull(
                          Objects.requireNonNull(valueType.getMapType()).getKeyType().getTypeName())
                      .toString()))
          .valueType(
              getOutputParameterType(Objects.requireNonNull(valueType.getMapType()).getValueType()))
          .type(ParameterTypeDefinition.TypeEnum.MAP);
    } else {
      throw new OutputProcessingException.WomtoolOutputTypeNotFoundException(valueType);
    }
  }

  public static List<WorkflowOutputDefinition> womtoolToCbasOutputs(WorkflowDescription womOutputs)
      throws OutputProcessingException.WomtoolOutputTypeNotFoundException {
    List<WorkflowOutputDefinition> cbasOutputs = new ArrayList<>();

    for (ToolOutputParameter output : womOutputs.getOutputs()) {
      WorkflowOutputDefinition workflowOutputDefinition = new WorkflowOutputDefinition();

      // Name
      String workflowName = womOutputs.getName();
      workflowOutputDefinition.outputName("%s.%s".formatted(workflowName, output.getName()));

      // ValueType
      workflowOutputDefinition.outputType(getOutputParameterType(output.getValueType()));

      // Destination
      workflowOutputDefinition.destination(new OutputDestinationNone());

      cbasOutputs.add(workflowOutputDefinition);
    }

    return cbasOutputs;
  }
}
