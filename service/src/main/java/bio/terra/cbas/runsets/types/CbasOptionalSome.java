package bio.terra.cbas.runsets.types;

public class CbasOptionalSome implements CbasOptional {

  private final CbasValue value;

  public CbasOptionalSome(CbasValue value) {
    this.value = value;
  }

  @Override
  public Object asSerializableValue() {
    return value.asSerializableValue();
  }
}
