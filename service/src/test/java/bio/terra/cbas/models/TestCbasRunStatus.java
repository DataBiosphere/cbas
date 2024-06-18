package bio.terra.cbas.models;

import static bio.terra.cbas.models.CbasRunStatus.fromCromwellStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TestCbasRunStatus {

  @ParameterizedTest(name = "")
  @CsvSource({
    "Submitted,INITIALIZING",
    "Running,RUNNING",
    "Aborting,CANCELING",
    "Aborted,CANCELED",
    "Failed,EXECUTOR_ERROR",
    "Succeeded,COMPLETE",
    "MyCromwellStatus,UNKNOWN"
  })
  void testFromCromwellStatus(String cromwellStatus, CbasRunStatus cbasStatus) {
    assertEquals(cbasStatus, fromCromwellStatus(cromwellStatus));
  }
}
