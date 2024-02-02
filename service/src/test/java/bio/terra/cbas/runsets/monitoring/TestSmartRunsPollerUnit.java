package bio.terra.cbas.runsets.monitoring;

import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class TestSmartRunsPollerUnit {

  private final UUID runId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private final UUID runId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private final UUID runId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private final UUID runId4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
  private final UUID runId5 = UUID.fromString("00000000-0000-0000-0000-000000000005");

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
        null,
        null);
  }
}
