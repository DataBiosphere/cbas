package bio.terra.cbas.initialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dao.util.ContainerizedDaoTest;
import bio.terra.cbas.models.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

public class TestCloneRecoveryService extends ContainerizedDaoTest {
  @Autowired MethodDao methodDao;
  @Autowired RunSetDao runSetDao;
  @Autowired RunDao runDao;
  @Autowired MethodVersionDao methodVersionDao;
  @Mock CbasContextConfiguration cbasContextConfig;

  @BeforeEach
  void init() {
    when(cbasContextConfig.getWorkspaceId()).thenReturn(currentWorkspaceId);
    when(cbasContextConfig.getWorkspaceCreatedDate()).thenReturn(currentWorkspaceCreatedDate);
  }

  //  @Test
  //  void testUpdateMethodTemplate() {
  //    methodDao.createMethod(clonedMethod);
  //    methodVersionDao.createMethodVersion(clonedMethodVersion);
  //    runSetDao.createRunSet(clonedTemplate);
  //
  //    runSetDao.createRunSet(clonedRunSet);
  //    runDao.createRun(clonedRun);
  //
  //    runSetDao.createRunSet(clonedRunSetLatest);
  //    runDao.createRun(clonedRunLatest);
  //
  //    runSetDao.createRunSet(currentRunSet);
  //    runDao.createRun(currentRun);
  //
  //    CloneRecoveryService cloneRecoveryService =
  //        new CloneRecoveryService(runSetDao, runDao, methodDao, cbasContextConfig);
  //
  //    cloneRecoveryService.updateMethodTemplate(clonedMethod);
  //    RunSet formerTemplate = runSetDao.getRunSet(clonedTemplate.runSetId());
  //    RunSet updatedRunSet = runSetDao.getRunSet(clonedRunSetLatest.runSetId());
  //    RunSet clonedRunSetRemaining = runSetDao.getRunSet(clonedRunSet.runSetId());
  //    RunSet currentRunSetRemaining = runSetDao.getRunSet(currentRunSet.runSetId());
  //
  //    assertEquals(false, formerTemplate.isTemplate());
  //    assertEquals(true, updatedRunSet.isTemplate());
  //    assertEquals(false, clonedRunSetRemaining.isTemplate());
  //    assertEquals(false, currentRunSetRemaining.isTemplate());
  //  }

  @Test
  void testRecoveryFromWorkspaceCloning() {
    methodDao.createMethod(clonedMethod);
    methodVersionDao.createMethodVersion(clonedMethodVersion);

    runSetDao.createRunSet(clonedTemplate);

    runSetDao.createRunSet(clonedRunSet);
    runDao.createRun(clonedRun);

    List<RunSet> initialRunSets = runSetDao.getRunSetsWithMethodId(clonedMethod.methodId());
    List<Run> initialRuns = runDao.getRuns(new RunDao.RunsFilters(clonedRunSet.runSetId(), null));
    List<RunSet> initialTemplates = runSetDao.getRunSets(null, true);

    assertEquals(2, initialRunSets.size());
    assertEquals(1, initialRuns.size());
    assertEquals(1, initialTemplates.size());
    assertEquals(clonedTemplate.runSetId(), initialTemplates.get(0).runSetId());

    CloneRecoveryService cloneRecoveryService =
        new CloneRecoveryService(runSetDao, runDao, methodDao, cbasContextConfig);
    cloneRecoveryService.cloneRecovery();

    List<RunSet> finalRunSets = runSetDao.getRunSetsWithMethodId(clonedMethod.methodId());
    List<Run> finalRuns = runDao.getRuns(new RunDao.RunsFilters(clonedRunSet.runSetId(), null));
    List<RunSet> finalTemplates = runSetDao.getRunSets(null, true);

    assertEquals(1, finalRunSets.size());
    assertEquals(0, finalRuns.size());
    assertEquals(1, finalTemplates.size());
    assertEquals(clonedRunSet.runSetId(), finalTemplates.get(0).runSetId());
  }

  @Test
  void testRecoveryFromAppUpgrade() {
    methodDao.createMethod(clonedMethod);
    methodVersionDao.createMethodVersion(clonedMethodVersion);

    runSetDao.createRunSet(clonedTemplate);

    runSetDao.createRunSet(clonedRunSet);
    runDao.createRun(clonedRun);

    runSetDao.createRunSet(currentRunSet);
    runDao.createRun(currentRun);
    methodDao.updateLastRunWithRunSet(currentRunSet);
    methodVersionDao.updateLastRunWithRunSet(currentRunSet);

    List<RunSet> initialRunSets = runSetDao.getRunSets(null, false);
    List<RunSet> initialTemplates = runSetDao.getRunSets(null, true);
    List<Run> initialRuns = runDao.getRuns(RunDao.RunsFilters.empty());

    assertEquals(2, initialRunSets.size());
    assertEquals(2, initialRuns.size());
    assertEquals(1, initialTemplates.size());
    assertEquals(false, runSetDao.getRunSet(clonedRunSet.runSetId()).isTemplate());
    assertEquals(false, runSetDao.getRunSet(currentRunSet.runSetId()).isTemplate());
    assertEquals(true, runSetDao.getRunSet(clonedTemplate.runSetId()).isTemplate());

    CloneRecoveryService cloneRecoveryService =
        new CloneRecoveryService(runSetDao, runDao, methodDao, cbasContextConfig);
    cloneRecoveryService.cloneRecovery();

    List<RunSet> finalRunSets = runSetDao.getRunSets(null, false);
    List<RunSet> finalTemplates = runSetDao.getRunSets(null, true);
    List<String> finalRunSetIds =
        Stream.concat(
                finalRunSets.stream().map(RunSet::runSetId),
                finalTemplates.stream().map(RunSet::runSetId))
            .map(UUID::toString)
            .toList();

    List<Run> finalRuns = runDao.getRuns(RunDao.RunsFilters.empty());

    assertEquals(1, finalRunSets.size());
    assertEquals(1, finalRuns.size());
    assertEquals(1, finalTemplates.size());
    assertEquals(true, runSetDao.getRunSet(clonedRunSet.runSetId()).isTemplate());
    assertEquals(false, runSetDao.getRunSet(currentRunSet.runSetId()).isTemplate());
    assertEquals(false, finalRunSetIds.contains(clonedTemplate.runSetId().toString()));
  }

  private final UUID originalWorkspaceId = UUID.randomUUID();
  private final OffsetDateTime originalWorkspaceCreationDate =
      OffsetDateTime.parse("2000-01-01T00:00:00.000000Z");
  private final UUID currentWorkspaceId = UUID.fromString("00000000-0000-0000-0000-000000000123");
  private final OffsetDateTime currentWorkspaceCreatedDate =
      OffsetDateTime.parse("2000-01-02T00:00:00.000000Z");

  Method clonedMethod =
      new Method(
          UUID.randomUUID(),
          "",
          "",
          currentWorkspaceCreatedDate.minusMinutes(5),
          null,
          "",
          originalWorkspaceId);

  MethodVersion clonedMethodVersion =
      new MethodVersion(
          UUID.randomUUID(),
          clonedMethod,
          "",
          "",
          currentWorkspaceCreatedDate.minusMinutes(5),
          null,
          "",
          originalWorkspaceId,
          "");

  RunSet clonedTemplate =
      new RunSet(
          // UUID.randomUUID(),
          UUID.fromString("00000000-0000-0000-0000-000000000001"),
          clonedMethodVersion,
          "",
          "",
          false,
          true,
          CbasRunSetStatus.COMPLETE,
          originalWorkspaceCreationDate.plusMinutes(1),
          originalWorkspaceCreationDate.plusMinutes(1),
          originalWorkspaceCreationDate.plusMinutes(1),
          0,
          0,
          "[]",
          "[]",
          "",
          "",
          originalWorkspaceId);

  RunSet clonedRunSet =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000002"),
          clonedMethodVersion,
          "",
          "",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          originalWorkspaceCreationDate.plusMinutes(5),
          originalWorkspaceCreationDate.plusMinutes(5),
          originalWorkspaceCreationDate.plusMinutes(5),
          0,
          0,
          "[]",
          "[]",
          "",
          "",
          originalWorkspaceId);

  Run clonedRun =
      new Run(
          UUID.randomUUID(),
          UUID.randomUUID().toString(),
          clonedRunSet,
          "",
          clonedRunSet.submissionTimestamp(),
          CbasRunStatus.COMPLETE,
          clonedRunSet.lastModifiedTimestamp(),
          clonedRunSet.lastPolledTimestamp(),
          "");

  RunSet clonedRunSetLatest =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000003"),
          clonedMethodVersion,
          "",
          "",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          originalWorkspaceCreationDate.plusMinutes(6),
          originalWorkspaceCreationDate.plusMinutes(6),
          originalWorkspaceCreationDate.plusMinutes(6),
          0,
          0,
          "[]",
          "[]",
          "",
          "",
          originalWorkspaceId);

  Run clonedRunLatest =
      new Run(
          UUID.randomUUID(),
          UUID.randomUUID().toString(),
          clonedRunSetLatest,
          "",
          clonedRunSetLatest.submissionTimestamp(),
          CbasRunStatus.COMPLETE,
          clonedRunSetLatest.lastModifiedTimestamp(),
          clonedRunSetLatest.lastPolledTimestamp(),
          "");

  RunSet currentRunSet =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000004"),
          clonedMethodVersion,
          "",
          "",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          currentWorkspaceCreatedDate.plusMinutes(10),
          currentWorkspaceCreatedDate.plusMinutes(10),
          currentWorkspaceCreatedDate.plusMinutes(10),
          0,
          0,
          "[]",
          "[]",
          "",
          "",
          currentWorkspaceId);

  Run currentRun =
      new Run(
          UUID.randomUUID(),
          UUID.randomUUID().toString(),
          currentRunSet,
          "",
          currentRunSet.submissionTimestamp(),
          CbasRunStatus.COMPLETE,
          currentRunSet.lastModifiedTimestamp(),
          currentRunSet.lastPolledTimestamp(),
          "");
}
