package bio.terra.cbas.runsets.types;

public class CbasString implements CbasValue {
  private final String value;

  /*
  Private constructor. Use 'parse' instead.
   */
  private CbasString(String value) {
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

  public static CbasString parse(String parameterName, Object value) throws TypeCoercionException {
    if (value instanceof String str) {
      return new CbasString(str);
    } else {
      throw new TypeCoercionException(parameterName, value, "String");
    }
  }
}
