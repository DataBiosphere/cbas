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

  @Override
  public long countFiles() {
    return 0L;
  }

  public static CbasBoolean parse(String parameterName, Object value) throws CoercionException {
    if (value instanceof Boolean b) {
      return new CbasBoolean(b);
    } else {
      throw new TypeCoercionException(parameterName, value, "Boolean");
    }
  }
}
