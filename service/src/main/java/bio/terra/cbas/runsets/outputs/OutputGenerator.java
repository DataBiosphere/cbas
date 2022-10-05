package bio.terra.cbas.runsets.outputs;

import bio.terra.cbas.model.WorkflowOutputDefinition;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;

public class OutputGenerator {
  public static RecordAttributes buildOutputs(
      List<WorkflowOutputDefinition> outputDefinitions, Object cromwellJsonObject) {
    RecordAttributes outputParams = new RecordAttributes();
    for (WorkflowOutputDefinition outputParam : outputDefinitions) {
      String parameterName = outputParam.getOutputName();
      Object outputValue;
      outputValue = ((Map<String, Object>) cromwellJsonObject).get(parameterName);
      String attributeName = outputParam.getRecordAttribute();

      outputParams.put(attributeName, outputValue);
    }
    return outputParams;
  }
}
