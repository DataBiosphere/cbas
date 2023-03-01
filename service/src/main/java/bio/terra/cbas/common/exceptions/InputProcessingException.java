package bio.terra.cbas.common.exceptions;

import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterTypeDefinition;

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
      super("Unsupported input source type: " + parameterSource.getType().toString());
    }
  }

  public static class StructMissingFieldException extends InputProcessingException {
    public StructMissingFieldException(String missingFieldName, String structName) {
      super(
          "Could not build a %s struct. Required field %s was not provided."
              .formatted(structName, missingFieldName));
    }
  }

  public static class InappropriateInputSourceException extends InputProcessingException {
    public InappropriateInputSourceException(
        ParameterDefinition sourceType, ParameterTypeDefinition valueType) {
      super(
          "Cannot use a source of type %s to specify an input value of type %s."
              .formatted(sourceType.getType(), valueType.getType()));
    }
  }
}
