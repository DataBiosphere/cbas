package bio.terra.cbas.runsets.types;

public abstract class CoercionException extends Exception {
  private final String parameterName;
  private final String fromType;
  private final String toType;
  private final String reason;

  protected CoercionException(String parameterName, String fromType, String toType, String reason) {
    this.parameterName = parameterName;
    this.fromType = fromType;
    this.toType = toType;
    this.reason = reason;
  }

  @Override
  public String getMessage() {
    return "Coercion from %s to %s failed for parameter %s. %s"
        .formatted(fromType, toType, parameterName, reason);
  }
}
