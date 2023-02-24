package bio.terra.cbas.runsets.inputs;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WomtoolInputTypeNotFoundException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowAttributesNotFoundException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowInputSourceNotSupportedException;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionNone;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.runsets.types.CbasValue;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import cromwell.client.model.ToolInputParameter;
import cromwell.client.model.ValueType;
import cromwell.client.model.WorkflowDescription;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.databiosphere.workspacedata.model.RecordResponse;

public class InputGenerator {

  private static final JsonMapper jsonMapper =
      // Json order doesn't really matter, but for test cases it's convenient to have them
      // be consistent.
      JsonMapper.builder()
          .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .build();

  public static ParameterTypeDefinition recursivelyGetParameterType(ValueType valueType)
      throws WomtoolInputTypeNotFoundException {

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
              recursivelyGetParameterType(Objects.requireNonNull(valueType.getOptionalType()))
                  .type(ParameterTypeDefinition.TypeEnum.OPTIONAL));
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.FILE)) {
      return new ParameterTypeDefinitionPrimitive()
          .primitiveType(PrimitiveParameterValueType.FILE)
          .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE);
    } else if (Objects.equals(valueType.getTypeName(), ValueType.TypeNameEnum.ARRAY)) {
      return new ParameterTypeDefinitionArray()
          .nonEmpty(valueType.getNonEmpty())
          .arrayType(
              recursivelyGetParameterType(Objects.requireNonNull(valueType.getArrayType()))
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
              recursivelyGetParameterType(
                  Objects.requireNonNull(valueType.getMapType()).getValueType()))
          .type(ParameterTypeDefinition.TypeEnum.MAP);
    } else {
      throw new WomtoolInputTypeNotFoundException(valueType);
    }
  }

  public static List<WorkflowInputDefinition> womToCbasInputBuilder(WorkflowDescription womInputs)
      throws WomtoolInputTypeNotFoundException {
    List<WorkflowInputDefinition> cbasInputDefinition = new ArrayList<>();
    String workflowName = womInputs.getName();

    for (ToolInputParameter input : womInputs.getInputs()) {
      WorkflowInputDefinition workflowInputDefinition = new WorkflowInputDefinition();

      // Name
      workflowInputDefinition.inputName("%s.%s".formatted(workflowName, input.getName()));

      // Input type
      workflowInputDefinition.inputType(recursivelyGetParameterType(input.getValueType()));

      // Source
      if (input.getDefault() == null) {
        workflowInputDefinition.source(new ParameterDefinitionNone());
      } else {
        workflowInputDefinition.source(
            new ParameterDefinitionLiteralValue()
                .parameterValue(input.getDefault())
                .type(ParameterDefinition.TypeEnum.LITERAL));
      }

      cbasInputDefinition.add(workflowInputDefinition);
    }

    return cbasInputDefinition;
  }

  public static Map<String, Object> buildInputs(
      List<WorkflowInputDefinition> inputDefinitions, RecordResponse recordResponse)
      throws CoercionException, InputProcessingException {
    Map<String, Object> params = new HashMap<>();
    for (WorkflowInputDefinition param : inputDefinitions) {
      String parameterName = param.getInputName();
      Object parameterValue;
      if (param.getSource() instanceof ParameterDefinitionLiteralValue literalValue) {
        parameterValue = literalValue.getParameterValue();
      } else if (param.getSource() instanceof ParameterDefinitionNone) {
        parameterValue = null;
      } else if (param.getSource() instanceof ParameterDefinitionRecordLookup recordLookup) {
        String attributeName = recordLookup.getRecordAttribute();

        if (((Map<String, Object>) recordResponse.getAttributes()).containsKey(attributeName)) {
          parameterValue = recordResponse.getAttributes().get(attributeName);
        } else {
          if (param.getInputType().getType().equals(ParameterTypeDefinition.TypeEnum.OPTIONAL)) {
            parameterValue = null;
          } else {
            throw new WorkflowAttributesNotFoundException(
                attributeName, recordResponse.getId(), parameterName);
          }
        }
      } else {
        throw new WorkflowInputSourceNotSupportedException(param.getSource());
      }

      if (parameterValue != null) {
        // Convert into an appropriate CbasValue:
        System.out.println("INPUT TYPE: " + param.getInputType());
        CbasValue cbasValue = CbasValue.parseValue(param.getInputType(), parameterValue);
        params.put(parameterName, cbasValue.asSerializableValue());
        System.out.println("PARAMS: " + params);
      }
    }
    return params;
  }

  public static String inputsToJson(Map<String, Object> inputs) throws JsonProcessingException {
    return jsonMapper.writeValueAsString(inputs);
  }

  private InputGenerator() {
    // Do not use. No construction necessary for static utility class.
  }
}
