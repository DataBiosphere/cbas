package bio.terra.cbas.common.validation;

public record CbasValidVoid() implements CbasVoidValidation {
  public static CbasValidVoid INSTANCE = new CbasValidVoid();
}
