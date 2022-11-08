package bio.terra.cbas.runsets.types;

public class ValueCoercionException extends CoercionException {
  public ValueCoercionException(Object badValue, String toType, String reason) {
    super(
        badValue.getClass().getSimpleName(),
        toType,
        "Coercion supported but failed: %s.".formatted(reason));
  }
}
