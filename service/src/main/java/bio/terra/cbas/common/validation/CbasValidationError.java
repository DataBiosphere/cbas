package bio.terra.cbas.common.validation;

import java.util.List;

public record CbasValidationError(List<String> errors) implements CbasVoidValidation {
  public static CbasValidationError of(String error) {
    return new CbasValidationError(List.of(error));
  }
}
