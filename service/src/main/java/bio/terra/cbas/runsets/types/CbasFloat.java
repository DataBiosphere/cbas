package bio.terra.cbas.runsets.types;

public class CbasFloat implements CbasValue {
  private final Double value;

  /*
  Private constructor. Use 'parse' instead.
   */
  private CbasFloat(Double value) {
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

  public static CbasFloat parse(Object value) throws CoercionException {
    if (value instanceof Float f) {
      return new CbasFloat(f.doubleValue());
    } else if (value instanceof Double d) {
      return new CbasFloat(d);
    } else {
      throw new TypeCoercionException(value, "Double");
    }
  }
}
