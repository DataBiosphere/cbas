package bio.terra.cbas.runsets.inputs;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.common.exceptions.InputProcessingException.InappropriateInputSourceException;
import bio.terra.cbas.common.exceptions.InputProcessingException.StructMissingFieldException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowAttributesNotFoundException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowInputSourceNotSupportedException;
import bio.terra.cbas.model.ObjectBuilderField;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionNone;
import bio.terra.cbas.model.ParameterDefinitionObjectBuilder;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionStruct;
import bio.terra.cbas.model.StructField;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.runsets.types.CbasValue;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InputGenerator {

  private static final JsonMapper jsonMapper =
      // Json order doesn't really matter, but for test cases it's convenient to have them
      // be consistent.
      JsonMapper.builder()
          .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .build();

  public static Map<String, Object> buildInputs(
      List<WorkflowInputDefinition> inputDefinitions, RecordResponse recordResponse)
      throws CoercionException, InputProcessingException {
    Map<String, Object> params = new HashMap<>();
    for (WorkflowInputDefinition param : inputDefinitions) {
      Object paramValue =
          buildInput(param.getInputName(), param.getInputType(), param.getSource(), recordResponse);
      if (paramValue != null) {
        params.put(param.getInputName(), paramValue);
      }
    }
    return params;
  }

  public static Object buildInput(
      String parameterName,
      ParameterTypeDefinition inputType,
      ParameterDefinition parameterSource,
      RecordResponse recordResponse)
      throws InputProcessingException, CoercionException {
    Object parameterValue;
    if (parameterSource instanceof ParameterDefinitionLiteralValue literalValue) {
      parameterValue = literalValue.getParameterValue();
    } else if (parameterSource instanceof ParameterDefinitionNone) {
      parameterValue = null;
    } else if (parameterSource instanceof ParameterDefinitionRecordLookup recordLookup) {
      parameterValue =
          handleRecordLookupInput(parameterName, inputType, recordResponse, recordLookup);
    } else if (parameterSource instanceof ParameterDefinitionObjectBuilder objectBuilderSource) {
      parameterValue = handleObjectBuilderInput(inputType, recordResponse, objectBuilderSource);
    } else {
      throw new WorkflowInputSourceNotSupportedException(parameterSource);
    }

    if (parameterValue != null) {
      // Convert into an appropriate CbasValue:
      CbasValue cbasValue = CbasValue.parseValue(parameterName, inputType, parameterValue);
      return cbasValue.asSerializableValue();
    } else {
      return null;
    }
  }

  @Nullable
  private static Object handleRecordLookupInput(
      String parameterName,
      ParameterTypeDefinition inputType,
      RecordResponse recordResponse,
      ParameterDefinitionRecordLookup recordLookup)
      throws WorkflowAttributesNotFoundException {
    Object parameterValue;
    String attributeName = recordLookup.getRecordAttribute();

    if (((Map<String, Object>) recordResponse.getAttributes()).containsKey(attributeName)) {
      parameterValue = recordResponse.getAttributes().get(attributeName);
    } else {
      if (inputType.getType().equals(ParameterTypeDefinition.TypeEnum.OPTIONAL)) {
        parameterValue = null;
      } else {
        throw new WorkflowAttributesNotFoundException(
            attributeName, recordResponse.getId(), parameterName);
      }
    }
    return parameterValue;
  }

  @NotNull
  private static Object handleObjectBuilderInput(
      ParameterTypeDefinition inputType,
      RecordResponse recordResponse,
      ParameterDefinitionObjectBuilder objectBuilderSource)
      throws InputProcessingException, CoercionException {
    Object parameterValue;
    Map<String, Object> fields = new HashMap<>();
    if (inputType instanceof ParameterTypeDefinitionStruct structInputType) {
      for (StructField structField : structInputType.getFields()) {
        ParameterDefinition fieldSource =
            objectBuilderSource.getFields().stream()
                .filter(f -> Objects.equals(f.getName(), structField.getFieldName()))
                .findFirst()
                .map(ObjectBuilderField::getSource)
                .orElseThrow(
                    () ->
                        new StructMissingFieldException(
                            structField.getFieldName(), structInputType.getName()));
        Object paramValue =
            buildInput(
                structField.getFieldName(),
                structField.getFieldType(),
                fieldSource,
                recordResponse);
        if (paramValue != null) {
          fields.put(structField.getFieldName(), paramValue);
        }
      }
      parameterValue = fields;
    } else {
      throw new InappropriateInputSourceException(objectBuilderSource, inputType);
    }
    return parameterValue;
  }

  public static String inputsToJson(Map<String, Object> inputs) throws JsonProcessingException {
    return jsonMapper.writeValueAsString(inputs);
  }

  private InputGenerator() {
    // Do not use. No construction necessary for static utility class.
  }
}
