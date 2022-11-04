package bio.terra.cbas.runsets.types;

public class CbasOptionalNone implements CbasOptional {
  @Override
  public Object asSerializableValue() {
    return null;
  }

  @Override
  public long countFiles() {
    return 0L;
  }
}
