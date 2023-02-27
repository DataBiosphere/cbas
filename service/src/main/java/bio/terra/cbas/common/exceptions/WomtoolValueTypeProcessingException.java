package bio.terra.cbas.common.exceptions;

import cromwell.client.model.ValueType;
import java.util.Objects;

public class WomtoolValueTypeProcessingException extends Exception {

  public WomtoolValueTypeProcessingException(String message) { super(message); }

  public static class WomtoolValueTypeNotFoundException extends WomtoolValueTypeProcessingException {

    public WomtoolValueTypeNotFoundException(ValueType type) {
      super("Unsupported value type: " + Objects.requireNonNull(type.getTypeName()));
    }
  }
}
