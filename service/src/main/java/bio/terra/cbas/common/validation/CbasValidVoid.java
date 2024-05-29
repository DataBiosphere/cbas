package bio.terra.cbas.common.validation;

public record CbasValidVoid() implements CbasVoidValidation {
  public static final CbasValidVoid INSTANCE = new CbasValidVoid();
}
