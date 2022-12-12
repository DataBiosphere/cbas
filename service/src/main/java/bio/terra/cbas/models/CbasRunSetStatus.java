package bio.terra.cbas.models;

import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.util.Pair;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum CbasRunSetStatus {
  UNKNOWN("UNKNOWN"),
  QUEUED("QUEUED"),
  RUNNING("RUNNING"),
  COMPLETE("COMPLETE"),
  ERROR("ERROR"),
  CANCELED("CANCELED"),
  CANCELING("CANCELING");

  private final String value;

  public static final Set<CbasRunSetStatus> TERMINAL_STATES = EnumSet.of(CANCELED, COMPLETE, ERROR);

  public static final Set<CbasRunSetStatus> NON_TERMINAL_STATES =
      EnumSet.of(UNKNOWN, QUEUED, RUNNING, CANCELING);

  CbasRunSetStatus(String value) {
    this.value = value;
  }

  public boolean isTerminal() {
    return TERMINAL_STATES.contains(this);
  }

  public boolean nonTerminal() {
    return !isTerminal();
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

  public static CbasRunSetStatus fromRunStatuses(Map<CbasRunStatus, Integer> runStatusCounts) {

    // Prefer list of pairs over Map because it guarantees the right ordering:
    List<Pair<CbasRunStatus, CbasRunSetStatus>> statusMapping =
        List.of(
            new Pair<>(CbasRunStatus.UNKNOWN, CbasRunSetStatus.UNKNOWN),
            new Pair<>(CbasRunStatus.CANCELING, CbasRunSetStatus.CANCELING),
            new Pair<>(CbasRunStatus.RUNNING, CbasRunSetStatus.RUNNING),
            new Pair<>(CbasRunStatus.QUEUED, CbasRunSetStatus.RUNNING),
            new Pair<>(CbasRunStatus.PAUSED, CbasRunSetStatus.RUNNING),
            new Pair<>(CbasRunStatus.INITIALIZING, CbasRunSetStatus.RUNNING),
            new Pair<>(CbasRunStatus.SYSTEM_ERROR, CbasRunSetStatus.ERROR),
            new Pair<>(CbasRunStatus.EXECUTOR_ERROR, CbasRunSetStatus.ERROR),
            new Pair<>(CbasRunStatus.CANCELED, CbasRunSetStatus.CANCELED));

    for (Pair<CbasRunStatus, CbasRunSetStatus> p : statusMapping) {
      if (runStatusCounts.getOrDefault(p.a(), 0) != 0) {
        return p.b();
      }
    }

    // Nothing else matching, so the status is complete:
    return CbasRunSetStatus.COMPLETE;
  }
}
