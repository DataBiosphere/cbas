package bio.terra.cbas.runsets.types;

public class ValueCoercionException extends CoercionException {
  public ValueCoercionException(String fromType, String toType, String reason) {
    super(fromType, toType, "Coercion supported but failed: %s.".formatted(reason));
  }
}
