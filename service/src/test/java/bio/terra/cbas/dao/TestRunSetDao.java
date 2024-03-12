package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
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
class TestRunSetDao {

  @Autowired RunSetDao runSetDao;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;

  private final UUID workspaceId = UUID.randomUUID();

  Method method =
      new Method(
          UUID.fromString("00000000-0000-0000-0000-000000000008"),
          "fetch_sra_to_bam",
          "fetch_sra_to_bam",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "Github",
          workspaceId);

  MethodVersion methodVersion =
      new MethodVersion(
          UUID.fromString("80000000-0000-0000-0000-000000000008"),
          method,
          "1.0",
          "fetch_sra_to_bam sample submission",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl",
          workspaceId,
          "develop");

  RunSet runSet =
      new RunSet(
          UUID.fromString("10000000-0000-0000-0000-000000000008"),
          methodVersion,
          "fetch_sra_to_bam workflow",
          "fetch_sra_to_bam sample submission",
          false,
          true,
          CbasRunSetStatus.COMPLETE,
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

  @BeforeAll
  void init() {
    methodDao.createMethod(method);
    methodVersionDao.createMethodVersion(methodVersion);
    runSetDao.createRunSet(runSet);
  }

  @AfterAll
  void cleanup() {
    try {
      int recordsRunSetDeleted = runSetDao.deleteRunSet(runSet.runSetId());
      int recordsMethodVersionDeleted =
          methodVersionDao.deleteMethodVersion(methodVersion.methodVersionId());
      int recordsMethodDeleted = methodDao.deleteMethod(method.methodId());

      assertEquals(1, recordsRunSetDeleted);
      assertEquals(1, recordsMethodDeleted);
      assertEquals(1, recordsMethodVersionDeleted);
    } catch (Exception ex) {
      fail("Failure while removing test run set record from a database", ex);
    }
  }

  @Test
  void retrievesSingleRunSet() {
    RunSet actual = runSetDao.getRunSet(UUID.fromString("10000000-0000-0000-0000-000000000008"));

    assertEquals(runSet.runSetId(), actual.runSetId());
    assertEquals(
        runSet.methodVersion().methodVersionId(), actual.methodVersion().methodVersionId());
    assertEquals(runSet.name(), actual.name());
    assertEquals(runSet.description(), actual.description());
    assertEquals(runSet.status(), actual.status());
    assertEquals(runSet.runCount(), actual.runCount());
    assertEquals(runSet.errorCount(), actual.errorCount());
    assertEquals(runSet.recordType(), actual.recordType());
  }

  @Test
  void retrievesAllRunSets() {
    // DB has no non-templated run sets, so it will return empty list
    List<RunSet> runSets = runSetDao.getRunSets(null, false);
    assertEquals(0, runSets.size());

    List<RunSet> templateRunSets = runSetDao.getRunSets(2, true);
    assertEquals(1, templateRunSets.size());
  }
}
