package bio.terra.cbas.runsets.types;

public class CbasOptionalNone implements CbasOptional {
  @Override
  public Object asCromwellInput() {
    return null;
  }
}
