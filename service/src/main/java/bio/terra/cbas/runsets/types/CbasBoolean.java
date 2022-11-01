package bio.terra.cbas.runsets.types;

public class CbasBoolean implements CbasValue {
  private final Boolean value;

  /*
  Private constructor. Use 'parse' instead.
   */
  private CbasBoolean(Boolean value) {
    this.value = value;
  }

  @Override
  public Object asSerializableValue() {
    return value;
  }

  public static CbasBoolean parse(Object value) throws CoercionException {
    if (value instanceof Boolean b) {
      return new CbasBoolean(b);
    } else {
      throw new TypeCoercionException(value, "Int");
    }
  }
}
