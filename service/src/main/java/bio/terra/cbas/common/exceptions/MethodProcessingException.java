package bio.terra.cbas.common.exceptions;

public class MethodProcessingException extends Exception {

  public MethodProcessingException(String message) {
    super(message);
  }

  public static class UnknownMethodSourceException extends MethodProcessingException {
    public UnknownMethodSourceException(String methodSource) {
      super("Unknown method source: " + methodSource);
    }
  }
}
