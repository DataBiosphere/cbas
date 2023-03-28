package bio.terra.cbas.util.methods;

import bio.terra.cbas.common.exceptions.WomtoolValueTypeProcessingException.WomtoolValueTypeNotFoundException;
import bio.terra.cbas.model.MethodInputMapping;
import bio.terra.cbas.model.MethodOutputMapping;
import bio.terra.cbas.model.OutputDestination;
import bio.terra.cbas.model.OutputDestinationNone;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionNone;
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
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.ToolOutputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.ValueTypeObjectFieldTypesInner;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class WomtoolToCbasInputsAndOutputs {

  private WomtoolToCbasInputsAndOutputs() {
    throw new UnsupportedOperationException("Cannot be instantiated");
  }

  public static ParameterTypeDefinition getParameterType(ValueType valueType)
      throws WomtoolValueTypeNotFoundException {

    List<StructField> fields = new ArrayList<>();

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
          .optionalType(getParameterType(Objects.requireNonNull(valueType.getOptionalType())))
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
      case OBJECT -> {
        for (ValueTypeObjectFieldTypesInner innerField :
            Objects.requireNonNull(valueType.getObjectFieldTypes())) {
          StructField structField = new StructField();
          structField.fieldName(innerField.getFieldName());
          structField.fieldType(
              getParameterType(Objects.requireNonNull(innerField.getFieldType())));
          fields.add(structField);
        }
        yield new ParameterTypeDefinitionStruct()
            .name("Struct")
            .fields(fields)
            .type(ParameterTypeDefinition.TypeEnum.STRUCT);
      }
      default -> throw new WomtoolValueTypeNotFoundException(valueType);
    };
  }

  public static ParameterTypeDefinition getInputType(boolean isOptional, ValueType inputValueType)
      throws WomtoolValueTypeNotFoundException {
    if (isOptional && inputValueType.getTypeName() != ValueType.TypeNameEnum.OPTIONAL) {
      return new ParameterTypeDefinitionOptional()
          .optionalType(getParameterType(Objects.requireNonNull(inputValueType)))
          .type(ParameterTypeDefinition.TypeEnum.OPTIONAL);
    } else {
      return getParameterType(inputValueType);
    }
  }

  public static ParameterDefinition getSource(
      String inputName,
      String defaultValue,
      Map<String, ParameterDefinition> methodInputMappingMap) {
    if (methodInputMappingMap.containsKey(inputName)) {
      return methodInputMappingMap.get(inputName);
    } else {
      return new ParameterDefinitionNone().type(ParameterDefinition.TypeEnum.NONE);
    }
  }

  public static List<WorkflowInputDefinition> womToCbasInputBuilder(
      WorkflowDescription womInputs, List<MethodInputMapping> methodInputMappings)
      throws WomtoolValueTypeNotFoundException {
    List<WorkflowInputDefinition> cbasInputDefinition = new ArrayList<>();
    String workflowName = womInputs.getName();

    Map<String, ParameterDefinition> methodInputMappingAsMap =
        Optional.ofNullable(methodInputMappings).orElse(List.of()).stream()
            .collect(
                Collectors.toMap(MethodInputMapping::getInputName, MethodInputMapping::getSource));

    for (ToolInputParameter input : womInputs.getInputs()) {
      WorkflowInputDefinition workflowInputDefinition = new WorkflowInputDefinition();
      String workflowInputName = "%s.%s".formatted(workflowName, input.getName());

      // Name
      workflowInputDefinition.inputName(workflowInputName);

      // Input type
      workflowInputDefinition.inputType(getInputType(input.getOptional(), input.getValueType()));

      // Source
      workflowInputDefinition.source(
          getSource(workflowInputName, input.getDefault(), methodInputMappingAsMap));

      cbasInputDefinition.add(workflowInputDefinition);
    }

    return cbasInputDefinition;
  }

  public static OutputDestination getDestination(
      String outputName, Map<String, OutputDestination> methodOutputMappingMap) {
    if (methodOutputMappingMap.containsKey(outputName)) {
      return methodOutputMappingMap.get(outputName);
    } else {
      return new OutputDestinationNone().type(OutputDestination.TypeEnum.NONE);
    }
  }

  // Outputs
  public static List<WorkflowOutputDefinition> womToCbasOutputBuilder(
      WorkflowDescription womOutputs, List<MethodOutputMapping> methodOutputMappings)
      throws WomtoolValueTypeNotFoundException {
    List<WorkflowOutputDefinition> cbasOutputs = new ArrayList<>();
    String workflowName = womOutputs.getName();

    Map<String, OutputDestination> methodOutputMappingAsMap =
        Optional.ofNullable(methodOutputMappings).orElse(List.of()).stream()
            .collect(
                Collectors.toMap(
                    MethodOutputMapping::getOutputName, MethodOutputMapping::getDestination));

    for (ToolOutputParameter output : womOutputs.getOutputs()) {
      WorkflowOutputDefinition workflowOutputDefinition = new WorkflowOutputDefinition();
      String workflowOutputName = "%s.%s".formatted(workflowName, output.getName());

      // Name
      workflowOutputDefinition.outputName(workflowOutputName);

      // ValueType
      workflowOutputDefinition.outputType(getParameterType(output.getValueType()));

      // Destination
      workflowOutputDefinition.destination(
          getDestination(workflowOutputName, methodOutputMappingAsMap));

      cbasOutputs.add(workflowOutputDefinition);
    }

    return cbasOutputs;
  }
}
