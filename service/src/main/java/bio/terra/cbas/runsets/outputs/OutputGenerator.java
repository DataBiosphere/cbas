package bio.terra.cbas.runsets.outputs;

import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.common.exceptions.OutputProcessingException.WorkflowOutputDestinationNotSupportedException;
import bio.terra.cbas.common.exceptions.OutputProcessingException.WorkflowOutputNotFoundException;
import bio.terra.cbas.model.OutputDestinationNone;
import bio.terra.cbas.model.OutputDestinationRecordUpdate;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.runsets.types.CbasValue;
import bio.terra.cbas.runsets.types.CoercionException;
import java.util.List;
import java.util.Map;
import org.databiosphere.workspacedata.model.RecordAttributes;

public class OutputGenerator {

  @SuppressWarnings("unchecked")
  public static RecordAttributes buildOutputs(
      List<WorkflowOutputDefinition> outputDefinitions,
      Object cromwellOutputs,
      MicrometerMetrics micrometerMetrics)
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

      var coercedValue =
          CbasValue.parseValue(outputName, outputDefinition.getOutputType(), outputValue);
      micrometerMetrics.increaseEventCounter("files_updated_in_wds", coercedValue.countFiles());

      outputRecordAttributes.put(attributeName, coercedValue.asSerializableValue());
    }
    return outputRecordAttributes;
  }
}
