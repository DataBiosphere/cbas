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

  public static CbasString parse(Object value) {
    return new CbasString(value.toString());
  }
}
