package bio.terra.cbas.initialization;

import static org.junit.jupiter.api.Assertions.*;
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

public class TestCloneRecoveryServiceUnits {
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
    assertEquals(1, manifest.keepAsTemplate().size());
    assertEquals(2, manifest.toBeDeleted().size());
    assertTrue(manifest.keepAsTemplate().contains(clonedRunSetLatest));
    assertTrue(manifest.toBeDeleted().contains(clonedTemplate));
    assertTrue(manifest.toBeDeleted().contains(clonedRunSet));

    // current workspace run sets should not appear in the manifest
    assertFalse(manifest.toBeDeleted().contains(currentRunSet));
    assertFalse(manifest.keepAsTemplate().contains(currentRunSet));
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
