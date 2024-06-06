package bio.terra.cbas.models;

import bio.terra.cbas.model.RunSetState;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum CbasMethodStatus {
  ARCHIVED("ARCHIVED"),
  ACTIVE("ACTIVE");

  private final String value;

  CbasMethodStatus(String value) {
    this.value = value;
  }

  @JsonCreator
  public static CbasMethodStatus fromValue(String text) {
    for (CbasMethodStatus b : CbasMethodStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }

  public static CbasMethodStatus fromValue(RunSetState runSetState) {
    return fromValue(runSetState.toString());
  }
}
