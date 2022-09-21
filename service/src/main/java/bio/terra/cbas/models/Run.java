package bio.terra.cbas.models;

import bio.terra.cbas.model.RunState;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Run(
    UUID id,
    String engineId,
    RunSet runSet,
    String entityId,
    OffsetDateTime submissionTimestamp,
    String status) {

  public UUID getRunSetId() {
    return runSet.id();
  }

  public Run withStatus(String newStatus) {
    return new Run(id, engineId, runSet, entityId, submissionTimestamp, newStatus);
  }

  public boolean isTerminal() {
    RunState state = RunState.fromValue(this.status());
    return RunState.CANCELED.equals(state)
        || RunState.COMPLETE.equals(state)
        || RunState.EXECUTOR_ERROR.equals(state)
        || RunState.SYSTEM_ERROR.equals(state);
  }

  public boolean nonTerminal() {
    return !isTerminal();
  }
}
