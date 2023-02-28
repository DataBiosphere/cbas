package bio.terra.cbas.util.methods;

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

  public static ParameterTypeDefinition getParameterType(ValueType valueType)
      throws WomtoolValueTypeNotFoundException {

    return switch (Objects.requireNonNull(valueType.getTypeName())) {
      case STRING -> new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.STRING)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
      case INT -> new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.INT)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
      case BOOLEAN -> new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.BOOLEAN)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
      case FILE -> new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.FILE)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
      case FLOAT -> new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.FLOAT)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
      case OPTIONAL -> new ParameterTypeDefinitionOptional()
          .optionalType(
              getParameterType(Objects.requireNonNull(valueType.getOptionalType()))
                  .type(ParameterTypeDefinition.TypeEnum.OPTIONAL))
          .type(ParameterTypeDefinition.TypeEnum.OPTIONAL);
      case ARRAY -> new ParameterTypeDefinitionArray()
          .nonEmpty(valueType.getNonEmpty())
          .arrayType(getParameterType(Objects.requireNonNull(valueType.getArrayType())))
          .type(ParameterTypeDefinition.TypeEnum.ARRAY);
      case MAP -> new ParameterTypeDefinitionMap()
          .keyType(
              PrimitiveParameterValueType.fromValue(
                  Objects.requireNonNull(
                          Objects.requireNonNull(valueType.getMapType()).getKeyType().getTypeName())
                      .toString()))
          .valueType(
              getParameterType(Objects.requireNonNull(valueType.getMapType()).getValueType()))
          .type(ParameterTypeDefinition.TypeEnum.MAP);
      default -> throw new WomtoolValueTypeNotFoundException(valueType);
    };
  }

  public static List<WorkflowInputDefinition> womToCbasInputBuilder(WorkflowDescription womInputs)
      throws WomtoolValueTypeNotFoundException {
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
              .type(ParameterDefinition.TypeEnum.NONE));

      cbasInputDefinition.add(workflowInputDefinition);
    }

    return cbasInputDefinition;
  }

  // Outputs
  public static List<WorkflowOutputDefinition> womToCbasOutputBuilder(
      WorkflowDescription womOutputs) throws WomtoolValueTypeNotFoundException {
    List<WorkflowOutputDefinition> cbasOutputs = new ArrayList<>();

    for (ToolOutputParameter output : womOutputs.getOutputs()) {
      WorkflowOutputDefinition workflowOutputDefinition = new WorkflowOutputDefinition();

      // Name
      String workflowName = womOutputs.getName();
      workflowOutputDefinition.outputName("%s.%s".formatted(workflowName, output.getName()));

      // ValueType
      workflowOutputDefinition.outputType(getParameterType(output.getValueType()));

      // Destination
      workflowOutputDefinition.destination(
          new OutputDestinationNone().type(OutputDestination.TypeEnum.NONE));

      cbasOutputs.add(workflowOutputDefinition);
    }

    return cbasOutputs;
  }
}
