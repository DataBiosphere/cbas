package bio.terra.cbas.runsets.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TestSmartRunsPollerUnit {

  private final UUID runId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private final UUID runId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private final UUID runId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private final UUID runId4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
  private final UUID runId5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
  ;

  @Test
  void selectByLeastRecentlyPolled() {

    Run run1 =
        new Run(runId1, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(100), null);
    Run run2 =
        new Run(runId2, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(5), null);
    Run run3 =
        new Run(runId3, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(30), null);
    Run run4 =
        new Run(runId4, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(15), null);
    Run run5 =
        new Run(runId5, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(1), null);

    List<Run> inputSet = List.of(run1, run2, run3, run4, run5);

    SmartRunsPoller.PickedUpdatableRuns selectedForPoll =
        SmartRunsPoller.pickUpdatableRuns(inputSet, 3);

    assertEquals(
        new SmartRunsPoller.PickedUpdatableRuns(Set.of(run5, run2, run4), 5), selectedForPoll);
  }

  @Test
  void ignoreOldCompletedRuns() {

    Run run1 =
        new Run(
            runId1, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(10100), null);
    Run run2 =
        new Run(runId2, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(105), null);
    Run run3 =
        new Run(runId3, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(1030), null);
    Run run4 =
        new Run(runId4, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(1015), null);
    Run run5 =
        new Run(runId5, null, null, null, null, CbasRunStatus.RUNNING, null, timestamp(101), null);

    List<Run> inputSet =
        List.of(
            randomOldCompletedRun(),
            randomOldCompletedRun(),
            randomOldCompletedRun(),
            randomOldCompletedRun(),
            run1,
            randomOldCompletedRun(),
            run2,
            randomOldCompletedRun(),
            run3,
            randomOldCompletedRun(),
            run4,
            randomOldCompletedRun(),
            run5,
            randomOldCompletedRun(),
            randomOldCompletedRun(),
            randomOldCompletedRun(),
            randomOldCompletedRun());

    SmartRunsPoller.PickedUpdatableRuns selectedForPoll =
        SmartRunsPoller.pickUpdatableRuns(inputSet, 3);

    assertEquals(
        new SmartRunsPoller.PickedUpdatableRuns(Set.of(run5, run2, run4), 5), selectedForPoll);
  }

  /**
   * Convenience method to get an offsetdatetime from a long value to reduce copy/paste in main test
   * methods.
   *
   * @param epochSecond "seconds since epoch" for this timestamp to represent.
   * @return The OffsetDateTime.
   */
  private static OffsetDateTime timestamp(long epochSecond) {
    return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
  }

  private static Run randomOldCompletedRun() {
    return new Run(
        UUID.randomUUID(),
        null,
        null,
        null,
        null,
        CbasRunStatus.COMPLETE,
        null,
        timestamp(10100),
        null);
  }
}
