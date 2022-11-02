package bio.terra.cbas.runsets.types;

public class TypeCoercionException extends CoercionException {
  public TypeCoercionException(String fromType, String toType) {
    super(fromType, toType, "Coercion not supported between these types.");
  }

  public TypeCoercionException(Object badValue, String toType) {
    super(
        toType, badValue.getClass().getSimpleName(), "Coercion not supported between these types.");
  }
}
