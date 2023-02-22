package bio.terra.cbas.runsets.inputs;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowAttributesNotFoundException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowInputSourceNotSupportedException;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionNone;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
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

  public static List<WorkflowInputDefinition> womToCbasInputBuilder(WorkflowDescription womInputs) {
    List<WorkflowInputDefinition> cbasInputDefinition = new ArrayList<>();
    String workflowName = womInputs.getName();

    for (ToolInputParameter input : womInputs.getInputs()) {
      WorkflowInputDefinition workflowInputDefinition = new WorkflowInputDefinition();

      // Name
      // name_of_workflow.call_name.name_input --> used fetch-sra_to_bam in womtool endpoint and
      // compare to cbas db inputs
      workflowInputDefinition.inputName("%s.%s".formatted(workflowName, input.getName()));

      // Input type
      if (Objects.equals(input.getValueType().getTypeName(), ValueType.TypeNameEnum.STRING)) {
        workflowInputDefinition.inputType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.STRING)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE));
      } else if (Objects.equals(input.getValueType().getTypeName(), ValueType.TypeNameEnum.INT)) {
        workflowInputDefinition.inputType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.INT)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE));
      } else if (Objects.equals(
          input.getValueType().getTypeName(), ValueType.TypeNameEnum.BOOLEAN)) {
        workflowInputDefinition.inputType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.BOOLEAN)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE));
      } else if (Objects.equals(
          input.getValueType().getTypeName(), ValueType.TypeNameEnum.OPTIONAL)) {
        workflowInputDefinition.inputType(
            new ParameterTypeDefinitionOptional()
                .optionalType(
                    new ParameterTypeDefinitionOptional()
                        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                .type(ParameterTypeDefinition.TypeEnum.OPTIONAL));
      } else if (Objects.equals(input.getValueType().getTypeName(), ValueType.TypeNameEnum.FILE)) {
        workflowInputDefinition.inputType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.FILE)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE));
      } else if (Objects.equals(input.getValueType().getTypeName(), ValueType.TypeNameEnum.ARRAY)) {
        workflowInputDefinition.inputType(
            new ParameterTypeDefinitionArray()
                .nonEmpty(false)
                .arrayType(
                    new ParameterTypeDefinitionPrimitive()
                        .primitiveType(
                            PrimitiveParameterValueType.fromValue(
                                String.valueOf(input.getValueType())))
                        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                .type(ParameterTypeDefinition.TypeEnum.ARRAY));
      } else if (Objects.equals(input.getValueType().getTypeName(), ValueType.TypeNameEnum.FLOAT)) {
        workflowInputDefinition.inputType(
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.FLOAT)
                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE));
      }

      // Source
      if (input.getDefault() == null) {
        workflowInputDefinition.source(new ParameterDefinitionNone());
      } else {
        workflowInputDefinition.source(new ParameterDefinitionLiteralValue());
      }

      cbasInputDefinition.add(workflowInputDefinition);
    }

    return cbasInputDefinition;
  }

  public static Map<String, Object> buildInputs(
      List<WorkflowInputDefinition> inputDefinitions, RecordResponse recordResponse)
      throws CoercionException, InputProcessingException {
    // System.out.println("BUILDINS DEF: " + inputDefinitions);
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

      System.out.println("PARAM VAL: " + param.getInputType());
      if (parameterValue != null) {
        // Convert into an appropriate CbasValue:
        CbasValue cbasValue = CbasValue.parseValue(param.getInputType(), parameterValue);
        params.put(parameterName, cbasValue.asSerializableValue());
      }
    }
    System.out.println(params);
    return params;
  }

  public static String inputsToJson(Map<String, Object> inputs) throws JsonProcessingException {
    return jsonMapper.writeValueAsString(inputs);
  }

  private InputGenerator() {
    // Do not use. No construction necessary for static utility class.
  }
}
