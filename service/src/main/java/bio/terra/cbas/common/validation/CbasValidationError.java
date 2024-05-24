package bio.terra.cbas.common.validation;

import java.util.List;

public record CbasValidationError(List<String> errors) implements CbasVoidValidation {
  public CbasValidationError(String error) {
    this(List.of(error));
  }
}
