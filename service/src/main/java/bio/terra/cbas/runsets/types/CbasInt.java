package bio.terra.cbas.runsets.types;

public class CbasInt implements CbasValue {
  private final Long value;

  /*
  Private constructor. Use 'parse' instead.
   */
  private CbasInt(Long value) {
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

  public static CbasInt parse(Object value) throws CoercionException {
    if (value instanceof Integer i) {
      return new CbasInt(i.longValue());
    } else if (value instanceof Long l) {
      return new CbasInt(l);
    } else {
      throw new TypeCoercionException(value, "Int");
    }
  }
}
