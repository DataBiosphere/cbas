package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestRunDao {
  @Autowired RunDao runDao;
  @Autowired RunSetDao runSetDao;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;

  Method method =
      new Method(
          UUID.randomUUID(),
          "fetch_sra_to_bam_run_test",
          "fetch_sra_to_bam_run_test",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "Github");

  MethodVersion methodVersion =
      new MethodVersion(
          UUID.randomUUID(),
          method,
          "1.0",
          "fetch_sra_to_bam sample submission",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl");

  RunSet runSet =
      new RunSet(
          UUID.randomUUID(),
          methodVersion,
          "fetch_sra_to_bam_run_test workflow",
          "fetch_sra_to_bam_run_test sample submission",
          false,
          true,
          CbasRunSetStatus.RUNNING,
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          0,
          0,
          "[]",
          "[]",
          "sample",
          "user-foo");

  Run run =
      new Run(
          UUID.randomUUID(),
          null,
          runSet,
          null,
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          CbasRunStatus.INITIALIZING,
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null);

  @BeforeAll
  void init() {
    methodDao.createMethod(method);
    methodVersionDao.createMethodVersion(methodVersion);
    runSetDao.createRunSet(runSet);
    runDao.createRun(run);
  }

  @Test
  void getRunByIdIfExistsRetrievesSingleRun() {
    Optional<Run> result = runDao.getRunByIdIfExists(run.runId());
    Run actual = result.get();

    assertEquals(run.runSet().runSetId(), actual.runSet().runSetId());
    assertEquals(run.runId(), actual.runId());
    assertEquals(run.engineId(), actual.engineId());
    assertEquals(run.errorMessages(), actual.errorMessages());
    assertEquals(run.status(), actual.status());
  }

  @Test
  void getRunByIdIfExistsRunIdNotFound() {
    Optional<Run> result = runDao.getRunByIdIfExists(UUID.randomUUID());
    assertEquals(Optional.empty(), result);
  }

  @Test
  void getRunByIdIfExistsNullRunIdNotFound() {
    Optional<Run> result = runDao.getRunByIdIfExists(null);
    assertEquals(Optional.empty(), result);
  }
}
