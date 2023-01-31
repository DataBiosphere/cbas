package bio.terra.cbas.common.exceptions;

import bio.terra.cbas.model.ParameterDefinition;

public class InputProcessingException extends Exception {

  public InputProcessingException(String message) {
    super(message);
  }

  public static class WorkflowAttributesNotFoundException extends InputProcessingException {

    public WorkflowAttributesNotFoundException(
        String attribute, String recordId, String inputName) {
      super(
          "Attribute %s not found in WDS record %s (to populate workflow input %s)."
              .formatted(attribute, recordId, inputName));
    }
  }

  public static class WorkflowInputSourceNotSupportedException extends InputProcessingException {
    public WorkflowInputSourceNotSupportedException(ParameterDefinition parameterSource) {
      super("Unsupported input source type: " + parameterSource.getClass().getSimpleName());
    }
  }
}