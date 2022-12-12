package bio.terra.cbas.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestCbasRunSetStatus {

  @Test
  void noRuns() {

    Map<CbasRunStatus, Integer> runStatuses = Map.of();

    assertEquals(CbasRunSetStatus.COMPLETE, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void justRunningRuns() {

    var runStatuses = Map.of(CbasRunStatus.RUNNING, 10);

    assertEquals(CbasRunSetStatus.RUNNING, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void justCompletedRuns() {

    var runStatuses = Map.of(CbasRunStatus.COMPLETE, 10);

    assertEquals(CbasRunSetStatus.COMPLETE, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void runningAndCompletedRuns() {

    var runStatuses =
        Map.of(
            CbasRunStatus.RUNNING, 5,
            CbasRunStatus.COMPLETE, 5);

    assertEquals(CbasRunSetStatus.RUNNING, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void runningAndCompletedAndFailedRuns() {

    var runStatuses =
        Map.of(
            CbasRunStatus.RUNNING, 5,
            CbasRunStatus.SYSTEM_ERROR, 5,
            CbasRunStatus.COMPLETE, 5);

    assertEquals(CbasRunSetStatus.RUNNING, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void completedAndFailedRuns() {

    var runStatuses =
        Map.of(
            CbasRunStatus.SYSTEM_ERROR, 5,
            CbasRunStatus.COMPLETE, 5);

    assertEquals(CbasRunSetStatus.ERROR, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void completedAndExecutorFailedRuns() {

    var runStatuses =
        Map.of(
            CbasRunStatus.EXECUTOR_ERROR, 5,
            CbasRunStatus.COMPLETE, 5);

    assertEquals(CbasRunSetStatus.ERROR, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void canceledAndCompleteAndFailedRuns() {

    var runStatuses =
        Map.of(
            CbasRunStatus.EXECUTOR_ERROR, 5,
            CbasRunStatus.COMPLETE, 5,
            CbasRunStatus.CANCELED, 5);

    assertEquals(CbasRunSetStatus.CANCELED, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }

  @Test
  void cancelingAndCompleteAndFailedRuns() {

    var runStatuses =
        Map.of(
            CbasRunStatus.EXECUTOR_ERROR, 5,
            CbasRunStatus.COMPLETE, 5,
            CbasRunStatus.CANCELING, 5);

    assertEquals(CbasRunSetStatus.CANCELING, CbasRunSetStatus.fromRunStatuses(runStatuses));
  }
}
