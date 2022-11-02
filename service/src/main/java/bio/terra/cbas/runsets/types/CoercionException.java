package bio.terra.cbas.runsets.types;

public abstract class CoercionException extends Exception {
  private final String fromType;
  private final String toType;
  private final String reason;

  protected CoercionException(String fromType, String toType, String reason) {
    this.fromType = fromType;
    this.toType = toType;
    this.reason = reason;
  }

  @Override
  public String getMessage() {
    return "Coercion from %s to %s failed. %s".formatted(fromType, toType, reason);
  }
}
