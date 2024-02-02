package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
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

  String workspaceId = UUID.randomUUID().toString();

  Method method =
      new Method(
          UUID.randomUUID(),
          "fetch_sra_to_bam_run_test",
          "fetch_sra_to_bam_run_test",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "Github",
          workspaceId);

  MethodVersion methodVersion =
      new MethodVersion(
          UUID.randomUUID(),
          method,
          "1.0",
          "fetch_sra_to_bam sample submission",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl",
          workspaceId);

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
          "user-foo",
          workspaceId);

  Run run =
      new Run(
          UUID.randomUUID(),
          UUID.randomUUID().toString(),
          runSet,
          null,
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          CbasRunStatus.INITIALIZING,
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          workspaceId);

  @BeforeAll
  void init() {
    methodDao.createMethod(method);
    methodVersionDao.createMethodVersion(methodVersion);
  }

  @AfterAll
  void cleanup() {
    try {
      int recordsMethodVersionDeleted =
          methodVersionDao.deleteMethodVersion(methodVersion.methodVersionId());
      int recordsMethodDeleted = methodDao.deleteMethod(method.methodId());

      assertEquals(1, recordsMethodDeleted);
      assertEquals(1, recordsMethodVersionDeleted);
    } catch (Exception ex) {
      fail("Failure while removing test method record from a database", ex);
    }
  }

  @Test
  void getRunByEngineIdIfExistsRetrievesSingleRun() {
    try {
      runSetDao.createRunSet(runSet);
      runDao.createRun(run);

      List<Run> result = runDao.getRuns(new RunDao.RunsFilters(null, null, run.engineId()));
      assertNotNull(result);
      assertEquals(1, (long) result.size());
      assertTrue(result.stream().findFirst().isPresent());

      Run actual = result.stream().findFirst().get();

      assertEquals(run.runSet().runSetId(), actual.runSet().runSetId());
      assertEquals(run.runId(), actual.runId());
      assertEquals(run.engineId(), actual.engineId());
      assertEquals(run.errorMessages(), actual.errorMessages());
      assertEquals(run.status(), actual.status());
    } finally {
      try {
        int runsDeleted = runDao.deleteRun(run.runId());
        int runSetsDeleted = runSetDao.deleteRunSets(runSet.runSetId());

        assertEquals(1, runsDeleted);
        assertEquals(1, runSetsDeleted);
      } catch (Exception ex) {
        fail("Failure while removing test run from a database", ex);
      }
    }
  }

  @Test
  void getRunBySetRunIdAndEngineIdIfExistsRetrievesSingleRun() {
    try {
      runSetDao.createRunSet(runSet);
      runDao.createRun(run);

      List<Run> result =
          runDao.getRuns(new RunDao.RunsFilters(run.runSet().runSetId(), null, run.engineId()));
      assertNotNull(result);
      assertEquals(1, (long) result.size());
      assertTrue(result.stream().findFirst().isPresent());

      Run actual = result.stream().findFirst().get();

      assertEquals(run.runSet().runSetId(), actual.runSet().runSetId());
      assertEquals(run.runId(), actual.runId());
      assertEquals(run.engineId(), actual.engineId());
      assertEquals(run.errorMessages(), actual.errorMessages());
      assertEquals(run.status(), actual.status());
    } finally {
      try {
        int runsDeleted = runDao.deleteRun(run.runId());
        int runSetsDeleted = runSetDao.deleteRunSets(runSet.runSetId());

        assertEquals(1, runsDeleted);
        assertEquals(1, runSetsDeleted);
      } catch (Exception ex) {
        fail("Failure while removing test run from a database", ex);
      }
    }
  }

  @Test
  void getRunByEngineIdIfExistsEngineIdNotFound() {
    try {
      runSetDao.createRunSet(runSet);
      runDao.createRun(run);

      List<Run> result =
          runDao.getRuns(new RunDao.RunsFilters(null, null, UUID.randomUUID().toString()));
      assertEquals(Collections.emptyList(), result);
    } finally {
      try {
        int runsDeleted = runDao.deleteRun(run.runId());
        int runSetsDeleted = runSetDao.deleteRunSets(runSet.runSetId());

        assertEquals(1, runsDeleted);
        assertEquals(1, runSetsDeleted);
      } catch (Exception ex) {
        fail("Failure while removing test run from a database", ex);
      }
    }
  }

  @Test
  void getRunByEngineIdIfExistsNullEngineIdNotFound() {
    List<Run> result = runDao.getRuns(new RunDao.RunsFilters(null, null, null));
    assertEquals(Collections.emptyList(), result);
  }
}
