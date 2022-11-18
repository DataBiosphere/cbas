package bio.terra.cbas.models;

import bio.terra.cbas.model.RunSetState;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum CbasRunSetStatus {
  UNKNOWN("UNKNOWN"),
  QUEUED("QUEUED"),
  RUNNING("RUNNING"),
  COMPLETE("COMPLETE"),
  ERROR("ERROR"),
  CANCELED("CANCELED"),
  CANCELING("CANCELING");

  private final String value;

  CbasRunSetStatus(String value) {
    this.value = value;
  }

  @JsonCreator
  public static CbasRunSetStatus fromValue(String text) {
    for (CbasRunSetStatus b : CbasRunSetStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }

  public static CbasRunSetStatus fromValue(RunSetState runSetState) {
    return fromValue(runSetState.toString());
  }

  public static RunSetState toCbasRunSetApiState(CbasRunSetStatus cbasRunSetStatus) {
    return RunSetState.fromValue(cbasRunSetStatus.toString());
  }
}
