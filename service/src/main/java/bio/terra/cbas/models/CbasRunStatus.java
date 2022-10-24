package bio.terra.cbas.models;

import bio.terra.cbas.model.RunState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;

public enum CbasRunStatus {
  UNKNOWN("UNKNOWN"),
  QUEUED("QUEUED"),
  INITIALIZING("INITIALIZING"),
  RUNNING("RUNNING"),
  PAUSED("PAUSED"),
  COMPLETE("COMPLETE"),
  EXECUTOR_ERROR("EXECUTOR_ERROR"),
  SYSTEM_ERROR("SYSTEM_ERROR"),
  CANCELED("CANCELED"),
  CANCELING("CANCELING");

  private static final Set<CbasRunStatus> TERMINAL_STATES =
      EnumSet.of(CANCELED, COMPLETE, EXECUTOR_ERROR, SYSTEM_ERROR);

  private static final Set<CbasRunStatus> ERROR_STATES = EnumSet.of(EXECUTOR_ERROR, SYSTEM_ERROR);

  private final String value;

  CbasRunStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  public boolean isTerminal() {
    return TERMINAL_STATES.contains(this);
  }

  public boolean nonTerminal() {
    return !isTerminal();
  }

  public boolean inErrorState() {
    return ERROR_STATES.contains(this);
  }

  @JsonCreator
  public static CbasRunStatus fromValue(String text) {
    for (CbasRunStatus b : CbasRunStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }

  public static CbasRunStatus fromValue(@Nullable cromwell.client.model.State state) {
    if (state == null) {
      return UNKNOWN;
    } else {
      return fromValue(state.toString());
    }
  }

  public static CbasRunStatus fromValue(RunState runState) {
    return fromValue(runState.toString());
  }

  public static cromwell.client.model.State toWesState(CbasRunStatus cbasRunStatus) {
    return cromwell.client.model.State.fromValue(cbasRunStatus.toString());
  }

  public static RunState toCbasApiState(CbasRunStatus cbasRunStatus) {
    return RunState.fromValue(cbasRunStatus.toString());
  }
}
