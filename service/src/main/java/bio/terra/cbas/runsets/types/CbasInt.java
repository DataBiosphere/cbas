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

  public static CbasInt parse(String parameterName, Object value) throws CoercionException {
    if (value instanceof Integer i) {
      return new CbasInt(i.longValue());
    } else if (value instanceof Long l) {
      return new CbasInt(l);
    } else if (value instanceof Float f) {
      long longValue = Math.round(f);
      if (f == longValue) {
        return new CbasInt(longValue);
      } else {
        throw new TypeCoercionException(parameterName, value, "Int");
      }
    } else if (value instanceof Double d) {
      long longValue = Math.round(d);
      if (d == longValue) {
        return new CbasInt(longValue);
      } else {
        throw new TypeCoercionException(parameterName, value, "Int");
      }
    } else {
      throw new TypeCoercionException(parameterName, value, "Int");
    }
  }
}
