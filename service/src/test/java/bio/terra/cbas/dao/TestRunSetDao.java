package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.dao.util.ContainerizedDatabaseTest;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TestRunSetDao extends ContainerizedDatabaseTest {

  @Autowired RunSetDao runSetDao;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;

  @BeforeEach
  void init() {
    methodDao.createMethod(method);
    methodVersionDao.createMethodVersion(methodVersion);
    runSetDao.createRunSet(runSet);
  }

  private final UUID workspaceId = UUID.randomUUID();

  Method method =
      new Method(
          UUID.fromString("00000000-0000-0000-0000-000000000008"),
          "fetch_sra_to_bam",
          "fetch_sra_to_bam",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "Github",
          workspaceId,
          Optional.empty());

  UUID methodVersionId = UUID.fromString("80000000-0000-0000-0000-000000000008");
  String methodVersionGithash = "1234abc";
  MethodVersion methodVersion =
      new MethodVersion(
          methodVersionId,
          method,
          "1.0",
          "fetch_sra_to_bam sample submission",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl",
          workspaceId,
          "develop",
          Optional.of(new GithubMethodVersionDetails(methodVersionGithash, methodVersionId)));

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
    // Make sure the deep linking with method version details is working:
    assertEquals(
        methodVersionGithash, runSet.methodVersion().methodVersionDetails().get().githash());
  }

  @Test
  void retrievesAllRunSets() {
    // DB has no non-templated run sets, so it will return empty list
    List<RunSet> runSets = runSetDao.getRunSets(null, false);
    assertEquals(0, runSets.size());

    List<RunSet> templateRunSets = runSetDao.getRunSets(2, true);
    assertEquals(1, templateRunSets.size());

    List<RunSet> combined = new ArrayList<>(List.copyOf(runSets));
    combined.addAll(templateRunSets);

    // Make sure the deep linking with method version details is working:
    for (RunSet runSet : combined) {
      assertEquals(
          methodVersionGithash, runSet.methodVersion().methodVersionDetails().get().githash());
    }
  }

  @Test
  void getRunSetsWithMethodId() {
    // initially there is only one run set associated with this method
    assertEquals(
        runSet.runSetId(), runSetDao.getRunSetsWithMethodId(method.methodId()).get(0).runSetId());

    RunSet laterRunSet =
        new RunSet(
            UUID.randomUUID(),
            methodVersion,
            "fetch_sra_to_bam workflow",
            "fetch_sra_to_bam sample submission",
            false,
            false,
            CbasRunSetStatus.COMPLETE,
            runSet.submissionTimestamp().plusDays(1),
            runSet.lastModifiedTimestamp().plusDays(1),
            runSet.lastPolledTimestamp().plusDays(1),
            0,
            0,
            "[]",
            "[]",
            "sample",
            "user-foo",
            workspaceId);
    runSetDao.createRunSet(laterRunSet);

    RunSet latestRunSet =
        new RunSet(
            UUID.randomUUID(),
            methodVersion,
            "fetch_sra_to_bam workflow",
            "fetch_sra_to_bam sample submission",
            false,
            false,
            CbasRunSetStatus.COMPLETE,
            runSet.submissionTimestamp().plusDays(2),
            runSet.lastModifiedTimestamp().plusDays(2),
            runSet.lastPolledTimestamp().plusDays(2),
            0,
            0,
            "[]",
            "[]",
            "sample",
            "user-foo",
            workspaceId);

    runSetDao.createRunSet(latestRunSet);

    assertEquals(
        latestRunSet.runSetId(),
        runSetDao.getRunSetsWithMethodId(method.methodId()).get(0).runSetId());
    assertEquals(
        laterRunSet.runSetId(),
        runSetDao.getRunSetsWithMethodId(method.methodId()).get(1).runSetId());
    assertEquals(
        runSet.runSetId(), runSetDao.getRunSetsWithMethodId(method.methodId()).get(2).runSetId());

    assertEquals(3, runSetDao.getRunSetsWithMethodId(method.methodId()).size());

    assertEquals(
        methodVersionGithash, runSet.methodVersion().methodVersionDetails().get().githash());
  }

  @Test
  void updateIsTemplate() {
    runSetDao.updateIsTemplate(runSet.runSetId(), true);
    RunSet updatedRunSetIsTemplate = runSetDao.getRunSet(runSet.runSetId());
    assertTrue(updatedRunSetIsTemplate.isTemplate());

    runSetDao.updateIsTemplate(updatedRunSetIsTemplate.runSetId(), false);
    RunSet updatedRunSetIsNotTemplate = runSetDao.getRunSet(runSet.runSetId());
    assertFalse(updatedRunSetIsNotTemplate.isTemplate());
  }

  @Test
  void deleteRunSet() {
    int response = runSetDao.deleteRunSet(runSet.runSetId());
    assertEquals(1, response);
    assertThrows(IndexOutOfBoundsException.class, () -> runSetDao.getRunSet(runSet.runSetId()));
  }
}
