package bio.terra.cbas.initialization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.initialization.CloneRecoveryService.MethodTemplateUpdateManifest;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

public class UnitTestCloneRecoveryService {
  MethodDao methodDao;
  RunSetDao runSetDao;
  RunDao runDao;
  CbasContextConfiguration cbasContextConfig;

  @BeforeEach
  void setup() {
    cbasContextConfig = mock(CbasContextConfiguration.class);
    when(cbasContextConfig.getWorkspaceId()).thenReturn(currentWorkspaceId);
    when(cbasContextConfig.getWorkspaceCreatedDate()).thenReturn(currentWorkspaceCreatedDate);
  }

  @Test
  void generateTemplateUpdateManifest() {
    CloneRecoveryService cloneRecoveryService =
        new CloneRecoveryService(runSetDao, runDao, methodDao, cbasContextConfig);

    List<RunSet> methodRunSets =
        List.of(clonedRunSet, clonedRunSetLatest, clonedTemplate, currentRunSet);

    MethodTemplateUpdateManifest manifest =
        cloneRecoveryService.generateTemplateUpdateManifest(methodRunSets);

    // only cloned run sets should be listed in the manifest.
    // the latest cloned run set should be the new template.
    // all other clone run sets should be listed as non-templates.
    assertEquals(1, manifest.templateRunSets().size());
    assertEquals(2, manifest.nonTemplateRunSets().size());
    assertTrue(manifest.templateRunSets().contains(clonedRunSetLatest));
    assertTrue(manifest.nonTemplateRunSets().contains(clonedTemplate));
    assertTrue(manifest.nonTemplateRunSets().contains(clonedRunSet));

    // current workspace run sets should not appear in the manifest
    assertFalse(manifest.nonTemplateRunSets().contains(currentRunSet));
    assertFalse(manifest.templateRunSets().contains(currentRunSet));
  }

  @Test
  void getRunsToDelete() {
    runDao = mock(RunDao.class);

    Answer<List<Run>> answer =
        invocation -> {
          RunDao.RunsFilters filters = invocation.getArgument(0);
          if (filters.runSetId().equals(clonedRunSetLatest.runSetId())) {
            return List.of(clonedRunLatest);
          } else if (filters.runSetId().equals(clonedRunSet.runSetId())) {
            return List.of(clonedRun);
          } else if (filters.runSetId().equals(currentRunSet.runSetId())) {
            return List.of(currentRun);
          } else if (filters.runSetId().equals(clonedTemplate.runSetId())) {
            return List.of();
          } else {
            return List.of();
          }
        };

    when(runDao.getRuns(any(RunDao.RunsFilters.class))).thenAnswer(answer);

    CloneRecoveryService cloneRecoveryService =
        new CloneRecoveryService(runSetDao, runDao, methodDao, cbasContextConfig);

    List<RunSet> methodRunSets =
        List.of(clonedRunSet, clonedRunSetLatest, clonedTemplate, currentRunSet);

    List<Run> runsToDelete = cloneRecoveryService.getRunsToDelete(methodRunSets);

    assertEquals(2, runsToDelete.size());
    assertTrue(runsToDelete.contains(clonedRun));
    assertTrue(runsToDelete.contains(clonedRunLatest));
    assertFalse(runsToDelete.contains(currentRun));
  }

  @Test
  void getRunSetsToDelete() {
    CloneRecoveryService cloneRecoveryService =
        new CloneRecoveryService(runSetDao, runDao, methodDao, cbasContextConfig);

    List<RunSet> methodRunSets =
        List.of(clonedRunSet, clonedRunSetLatest, clonedTemplate, currentRunSet);

    List<RunSet> runSetsToDelete = cloneRecoveryService.getRunSetsToDelete(methodRunSets);
    assertEquals(2, runSetsToDelete.size());
    assertTrue(runSetsToDelete.contains(clonedRunSet));
    assertTrue(runSetsToDelete.contains(clonedRunSetLatest));
    assertFalse(runSetsToDelete.contains(clonedTemplate));
    assertFalse(runSetsToDelete.contains(currentRunSet));
  }

  private final UUID currentWorkspaceId = UUID.fromString("00000000-1111-1111-1111-000000000000");
  private final OffsetDateTime currentWorkspaceCreatedDate =
      OffsetDateTime.parse("2000-01-02T00:00:00.000000Z");
  private final UUID originalWorkspaceId = UUID.fromString("00000000-2222-2222-2222-000000000000");
  private final OffsetDateTime originalWorkspaceCreationDate =
      OffsetDateTime.parse("2000-01-01T00:00:00.000000Z");

  Method clonedMethod =
      new Method(
          UUID.randomUUID(),
          "clonedMethod",
          "",
          currentWorkspaceCreatedDate.minusMinutes(5),
          null,
          "",
          originalWorkspaceId);

  MethodVersion clonedMethodVersion =
      new MethodVersion(
          UUID.randomUUID(),
          clonedMethod,
          "clonedMethodVersion",
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
          "clonedTemplate",
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
          "clonedRunSet",
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

  UUID clonedRunSetLatestId = UUID.fromString("00000000-0000-0000-0000-000000000003");
  RunSet clonedRunSetLatest =
      new RunSet(
          clonedRunSetLatestId,
          clonedMethodVersion,
          "clonedRunSetLatest",
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
          "currentRunSet",
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
