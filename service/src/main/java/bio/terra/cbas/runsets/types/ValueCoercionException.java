package bio.terra.cbas.runsets.types;

public class ValueCoercionException extends CoercionException {
  public ValueCoercionException(
      String parameterName, Object badValue, String toType, String reason) {
    super(
        parameterName,
        badValue.getClass().getSimpleName(),
        toType,
        "Coercion supported but failed: %s.".formatted(reason));
  }
}
