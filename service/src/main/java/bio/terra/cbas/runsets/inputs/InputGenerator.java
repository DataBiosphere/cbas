package bio.terra.cbas.runsets.inputs;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowAttributesNotFoundException;
import bio.terra.cbas.common.exceptions.InputProcessingException.WorkflowInputSourceNotSupportedException;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionNone;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
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
import org.databiosphere.workspacedata.model.RecordResponse;

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
        CbasValue cbasValue = CbasValue.parseValue(param.getInputType(), parameterValue);
        params.put(parameterName, cbasValue.asSerializableValue());
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
