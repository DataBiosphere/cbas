package bio.terra.cbas.runsets.outputs;

import bio.terra.cbas.common.exceptions.WorkflowOutputNotFoundException;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;

public class OutputGenerator {

  public static RecordAttributes buildOutputs(
      List<WorkflowOutputDefinition> outputDefinitions, Object cromwellOutputs)
      throws WorkflowOutputNotFoundException {
    RecordAttributes outputRecordAttributes = new RecordAttributes();
    for (WorkflowOutputDefinition outputDefinition : outputDefinitions) {
      String outputName = outputDefinition.getOutputName();
      Object outputValue;

      if (!((Map<String, Object>) cromwellOutputs).containsKey(outputName)) {
        if (outputDefinition.getOutputType().getType()
            == ParameterTypeDefinition.TypeEnum.OPTIONAL) {
          outputValue = null;
        } else {
          throw new WorkflowOutputNotFoundException(
              String.format("Output %s not found in workflow outputs.", outputName));
        }
      } else {
        outputValue = ((Map<String, Object>) cromwellOutputs).get(outputName);
      }

      String attributeName = outputDefinition.getRecordAttribute();
      outputRecordAttributes.put(attributeName, outputValue);
    }
    return outputRecordAttributes;
  }
}
