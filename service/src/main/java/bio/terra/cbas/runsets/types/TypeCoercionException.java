package bio.terra.cbas.runsets.types;

public class TypeCoercionException extends CoercionException {
  public TypeCoercionException(String parameterName, String fromType, String toType) {
    super(parameterName, fromType, toType, "Coercion not supported between these types.");
  }

  public TypeCoercionException(String parameterName, Object badValue, String toType) {
    super(
        parameterName,
        badValue.getClass().getSimpleName(),
        toType,
        "Coercion not supported between these types.");
  }
}
