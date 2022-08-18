package bio.terra.cbas.runsets.inputs;

import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionEntityLookup;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.WorkflowParamDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.EntityResponse;

public class InputGenerator {
  public static Map<String, Object> buildInputs(
      List<WorkflowParamDefinition> inputDefinitions, EntityResponse entity) {
    Map<String, Object> params = new HashMap<>();
    for (WorkflowParamDefinition param : inputDefinitions) {
      String parameterName = param.getParameterName();
      Object parameterValue;
      if (param.getSource().getType() == ParameterDefinition.TypeEnum.LITERAL) {
        parameterValue = ((ParameterDefinitionLiteralValue) param.getSource()).getParameterValue();
      } else {
        String attributeName =
            ((ParameterDefinitionEntityLookup) param.getSource()).getEntityAttribute();
        parameterValue = entity.getAttributes().get(attributeName);
      }
      params.put(parameterName, parameterValue);
    }
    return params;
  }

  private InputGenerator() {
    // Do not use. No construction necessary for static utility class.
  }
}
