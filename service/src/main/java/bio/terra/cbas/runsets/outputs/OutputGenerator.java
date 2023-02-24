package bio.terra.cbas.runsets.outputs;

import static bio.terra.cbas.common.MetricsUtil.increaseEventCounter;

import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.common.exceptions.OutputProcessingException.WomtoolOutputTypeNotFoundException;
import bio.terra.cbas.common.exceptions.OutputProcessingException.WorkflowOutputDestinationNotSupportedException;
import bio.terra.cbas.common.exceptions.OutputProcessingException.WorkflowOutputNotFoundException;
import bio.terra.cbas.model.OutputDestinationNone;
import bio.terra.cbas.model.OutputDestinationRecordUpdate;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.runsets.types.CbasValue;
import bio.terra.cbas.runsets.types.CoercionException;
import cromwell.client.model.ToolOutputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.databiosphere.workspacedata.model.RecordAttributes;

public class OutputGenerator {

  public static ParameterTypeDefinition recursivelyGetOutputParameterType(ValueType valueType)
      throws WomtoolOutputTypeNotFoundException {
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
              recursivelyGetOutputParameterType(Objects.requireNonNull(valueType.getOptionalType()))
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
              recursivelyGetOutputParameterType(Objects.requireNonNull(valueType.getArrayType()))
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
              recursivelyGetOutputParameterType(
                  Objects.requireNonNull(valueType.getMapType()).getValueType()))
          .type(ParameterTypeDefinition.TypeEnum.MAP);
    } else {
      throw new WomtoolOutputTypeNotFoundException(valueType);
    }
  }

  public static List<WorkflowOutputDefinition> womtoolToCbasOutputs(WorkflowDescription womOutputs)
      throws WomtoolOutputTypeNotFoundException {
    List<WorkflowOutputDefinition> cbasOutputs = new ArrayList<>();

    for (ToolOutputParameter output : womOutputs.getOutputs()) {
      WorkflowOutputDefinition workflowOutputDefinition = new WorkflowOutputDefinition();

      // Name
      String workflowName = womOutputs.getName();
      workflowOutputDefinition.outputName("%s.%s".formatted(workflowName, output.getName()));

      // ValueType
      workflowOutputDefinition.outputType(recursivelyGetOutputParameterType(output.getValueType()));

      // Destination
      workflowOutputDefinition.destination(new OutputDestinationNone());

      cbasOutputs.add(workflowOutputDefinition);
    }

    return cbasOutputs;
  }

  public static RecordAttributes buildOutputs(
      List<WorkflowOutputDefinition> outputDefinitions, Object cromwellOutputs)
      throws OutputProcessingException, CoercionException {
    RecordAttributes outputRecordAttributes = new RecordAttributes();
    for (WorkflowOutputDefinition outputDefinition : outputDefinitions) {

      String attributeName;
      if (outputDefinition.getDestination() instanceof OutputDestinationNone) {
        continue;
      } else if (outputDefinition.getDestination() instanceof OutputDestinationRecordUpdate odru) {
        attributeName = odru.getRecordAttribute();
      } else {
        throw new WorkflowOutputDestinationNotSupportedException(outputDefinition.getDestination());
      }

      String outputName = outputDefinition.getOutputName();
      Object outputValue;

      if (!((Map<String, Object>) cromwellOutputs).containsKey(outputName)) {
        if (outputDefinition
            .getOutputType()
            .getType()
            .equals(ParameterTypeDefinition.TypeEnum.OPTIONAL)) {
          outputValue = null;
        } else {
          throw new WorkflowOutputNotFoundException(
              String.format("Output %s not found in workflow outputs.", outputName));
        }
      } else {
        outputValue = ((Map<String, Object>) cromwellOutputs).get(outputName);
      }

      var coercedValue = CbasValue.parseValue(outputDefinition.getOutputType(), outputValue);
      increaseEventCounter("files-updated-in-wds", coercedValue.countFiles());

      outputRecordAttributes.put(attributeName, coercedValue.asSerializableValue());
    }
    return outputRecordAttributes;
  }
}
