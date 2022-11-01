package bio.terra.cbas.runsets.inputs;

import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
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
      List<WorkflowInputDefinition> inputDefinitions, RecordResponse record)
      throws CoercionException {
    Map<String, Object> params = new HashMap<>();
    for (WorkflowInputDefinition param : inputDefinitions) {
      String parameterName = param.getInputName();
      Object parameterValue;
      if (param.getSource().getType() == ParameterDefinition.TypeEnum.LITERAL) {
        parameterValue = ((ParameterDefinitionLiteralValue) param.getSource()).getParameterValue();
      } else {
        String attributeName =
            ((ParameterDefinitionRecordLookup) param.getSource()).getRecordAttribute();
        parameterValue = record.getAttributes().get(attributeName);
      }

      // Convert into an appropriate CbasValue:
      CbasValue cbasValue = CbasValue.parseValue(param.getInputType(), parameterValue);

      params.put(parameterName, cbasValue.asCromwellInput());
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
