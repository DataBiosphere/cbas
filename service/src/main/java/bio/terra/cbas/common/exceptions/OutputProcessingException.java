package bio.terra.cbas.common.exceptions;

import bio.terra.cbas.model.OutputDestination;

public class OutputProcessingException extends Exception {

  public OutputProcessingException(String message) {
    super(message);
  }

  public static class WorkflowOutputNotFoundException extends OutputProcessingException {
    public WorkflowOutputNotFoundException(String message) {
      super(message);
    }
  }

  public static class WorkflowOutputDestinationNotSupportedException
      extends OutputProcessingException {
    public WorkflowOutputDestinationNotSupportedException(OutputDestination destination) {
      super("Unsupported output destination type: " + destination.getClass().getSimpleName());
    }
  }
}
