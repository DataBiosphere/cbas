package bio.terra.cbas.util.methods;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.util.BackfillOriginalWorkspaceIdService;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestBackfillOriginalWorkspaceIdService {
  RunSetDao runSetDao;
  MethodDao methodDao;
  MethodVersionDao methodVersionDao;
  CbasContextConfiguration cbasContextConfig;

  @BeforeEach
  void setup() {
    cbasContextConfig = mock(CbasContextConfiguration.class);
    runSetDao = mock(RunSetDao.class);
    methodDao = mock(MethodDao.class);
    methodVersionDao = mock(MethodVersionDao.class);

    when(cbasContextConfig.getWorkspaceId()).thenReturn(currentWorkspaceId);
    when(cbasContextConfig.getWorkspaceCreatedDate()).thenReturn(workspaceCreatedDate);
  }

  @Test
  void testBackfillMethods() {
    when(methodDao.getMethods())
        .thenReturn(
            Arrays.asList(
                methodClonedNull,
                methodCreatedNull,
                methodClonedBackfilled,
                methodCreatedBackfilled));
    when(methodDao.updateOriginalWorkspaceId(any(), any())).thenReturn(1);

    BackfillOriginalWorkspaceIdService backfillOriginalWorkspaceIdService =
        new BackfillOriginalWorkspaceIdService(
            cbasContextConfig, runSetDao, methodDao, methodVersionDao);
    backfillOriginalWorkspaceIdService.backfillMethods();

    verify(methodDao, times(1)).getMethods();
    verify(methodDao).updateOriginalWorkspaceId(methodClonedNull.methodId(), nullWorkspaceId);
    verify(methodDao).updateOriginalWorkspaceId(methodCreatedNull.methodId(), currentWorkspaceId);
    verify(methodDao, never())
        .updateOriginalWorkspaceId(eq(methodClonedBackfilled.methodId()), any());
    verify(methodDao, never())
        .updateOriginalWorkspaceId(eq(methodCreatedBackfilled.methodId()), any());
  }

  @Test
  void testBackfillRunSets() {
    when(runSetDao.getRunSets(eq(null), eq(false)))
        .thenReturn(
            Arrays.asList(
                runSetClonedNull,
                runSetCreatedNull,
                runSetClonedBackfilled,
                runSetCreatedBackfilled));
    when(runSetDao.getRunSets(eq(null), eq(true)))
        .thenReturn(
            Arrays.asList(
                templateClonedNull,
                templateCreatedNull,
                templateClonedBackfilled,
                templateCreatedBackfilled));
    when(runSetDao.updateOriginalWorkspaceId(any(), any())).thenReturn(1);

    BackfillOriginalWorkspaceIdService backfillOriginalWorkspaceIdService =
        new BackfillOriginalWorkspaceIdService(
            cbasContextConfig, runSetDao, methodDao, methodVersionDao);
    backfillOriginalWorkspaceIdService.backfillRunSets();

    verify(runSetDao, times(1)).getRunSets(null, false);
    verify(runSetDao, times(1)).getRunSets(null, true);
    verify(runSetDao).updateOriginalWorkspaceId(runSetClonedNull.runSetId(), nullWorkspaceId);
    verify(runSetDao).updateOriginalWorkspaceId(runSetCreatedNull.runSetId(), currentWorkspaceId);
    verify(runSetDao).updateOriginalWorkspaceId(templateClonedNull.runSetId(), nullWorkspaceId);
    verify(runSetDao).updateOriginalWorkspaceId(templateCreatedNull.runSetId(), currentWorkspaceId);
    verify(runSetDao, never())
        .updateOriginalWorkspaceId(eq(runSetClonedBackfilled.runSetId()), any());
    verify(runSetDao, never())
        .updateOriginalWorkspaceId(eq(runSetCreatedBackfilled.runSetId()), any());
    verify(runSetDao, never())
        .updateOriginalWorkspaceId(eq(templateClonedBackfilled.runSetId()), any());
    verify(runSetDao, never())
        .updateOriginalWorkspaceId(eq(templateCreatedBackfilled.runSetId()), any());
  }

  @Test
  void testBackfillMethodVersions() {
    when(methodVersionDao.getMethodVersions())
        .thenReturn(
            Arrays.asList(
                methodVersionClonedNull,
                methodVersionCreatedNull,
                methodVersionClonedBackfilled,
                methodVersionCreatedBackfilled));
    when(methodVersionDao.updateOriginalWorkspaceId(any(), any())).thenReturn(1);

    BackfillOriginalWorkspaceIdService backfillOriginalWorkspaceIdService =
        new BackfillOriginalWorkspaceIdService(
            cbasContextConfig, runSetDao, methodDao, methodVersionDao);
    backfillOriginalWorkspaceIdService.backfillMethodVersions();

    verify(methodVersionDao, times(1)).getMethodVersions();
    verify(methodVersionDao)
        .updateOriginalWorkspaceId(methodVersionClonedNull.methodVersionId(), nullWorkspaceId);
    verify(methodVersionDao)
        .updateOriginalWorkspaceId(methodVersionCreatedNull.methodVersionId(), currentWorkspaceId);
    verify(methodVersionDao, never())
        .updateOriginalWorkspaceId(eq(methodVersionClonedBackfilled.methodVersionId()), any());
    verify(methodVersionDao, never())
        .updateOriginalWorkspaceId(eq(methodVersionCreatedBackfilled.methodVersionId()), any());
  }

  private final OffsetDateTime creationDateBefore =
      OffsetDateTime.parse("1990-12-19T00:00:00.000000Z");
  private final OffsetDateTime workspaceCreatedDate =
      OffsetDateTime.parse("1990-12-20T00:00:00.000000Z");
  private final OffsetDateTime creationDateAfter =
      OffsetDateTime.parse("1990-12-21T00:00:00.000000Z");

  private final UUID currentWorkspaceId = UUID.fromString("f760e214-75f8-4afc-abdb-e9945d77a325");
  private final UUID nullWorkspaceId = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private Method methodClonedNull =
      new Method(
          UUID.fromString("00000000-0000-0000-0000-000000000010"),
          "MethodClonedNull",
          "A method that was cloned from a source workspace, with a null originalWorkspaceId",
          creationDateBefore,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "GitHub",
          null);
  private Method methodCreatedNull =
      new Method(
          UUID.fromString("00000000-0000-0000-0000-000000000020"),
          "MethodCreatedNull",
          "A method that was created in the current workspace, with a null originalWorkspaceId",
          creationDateAfter,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "GitHub",
          null);

  private Method methodClonedBackfilled =
      new Method(
          UUID.fromString("00000000-0000-0000-0000-000000000030"),
          "MethodClonedBackfilled",
          "A method that was cloned from a source workspace, with a backfilled originalWorkspaceId",
          creationDateBefore,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "GitHub",
          nullWorkspaceId);
  private Method methodCreatedBackfilled =
      new Method(
          UUID.fromString("00000000-0000-0000-0000-000000000040"),
          "MethodCreatedBackfilled",
          "A method that was created in the current workspace, with a backfilled originalWorkspaceId",
          creationDateAfter,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "GitHub",
          nullWorkspaceId);

  private MethodVersion methodVersionClonedNull =
      new MethodVersion(
          UUID.fromString("00000000-0000-0000-0000-000000000011"),
          methodClonedNull,
          "MethodVersionClonedNull",
          "A method version that was cloned from a source workspace, with a null originalWorkspaceId",
          creationDateBefore,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "http://wdlhub.biz",
          null,
          "branch",
          Optional.empty());

  private MethodVersion methodVersionCreatedNull =
      new MethodVersion(
          UUID.fromString("00000000-0000-0000-0000-000000000021"),
          methodCreatedNull,
          "MethodVersionCreatedNull",
          "A method version that was created in the current workspace, with a null originalWorkspaceId",
          creationDateAfter,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "http://wdlhub.biz",
          null,
          "branch",
          Optional.empty());

  private MethodVersion methodVersionClonedBackfilled =
      new MethodVersion(
          UUID.fromString("00000000-0000-0000-0000-000000000031"),
          methodClonedBackfilled,
          "MethodVersionClonedBackfilled",
          "A method version that was cloned from a source workspace, with a backfilled originalWorkspaceId",
          creationDateBefore,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "http://wdlhub.biz",
          nullWorkspaceId,
          "branch",
          Optional.empty());
  private MethodVersion methodVersionCreatedBackfilled =
      new MethodVersion(
          UUID.fromString("00000000-0000-0000-0000-000000000041"),
          methodCreatedBackfilled,
          "MethodVersionCreatedBackfilled",
          "A method version that was created in the current workspace, with a backfilled originalWorkspaceId",
          creationDateAfter,
          UUID.randomUUID(), // this can be random because it isn't relevant to these tests.
          "http://wdlhub.biz",
          nullWorkspaceId,
          "branch",
          Optional.empty());

  private RunSet runSetClonedNull =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000001"),
          methodVersionClonedNull,
          "RunSetClonedNull",
          "A run set that was cloned from a source workspace, with a null originalWorkspaceId",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          creationDateBefore,
          creationDateBefore,
          creationDateBefore,
          1,
          0,
          "",
          "",
          "",
          "",
          null);

  private RunSet runSetCreatedNull =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000002"),
          methodVersionCreatedNull,
          "RunSetCreatedNull",
          "A run set that was created in the current workspace, with a null originalWorkspaceId",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          creationDateAfter,
          creationDateAfter,
          creationDateAfter,
          1,
          0,
          "",
          "",
          "",
          "",
          null);

  private RunSet runSetClonedBackfilled =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000003"),
          methodVersionClonedBackfilled,
          "RunSetClonedBackfilled",
          "A run set that was cloned from a source workspace, with a backfilled originalWorkspaceId",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          creationDateBefore,
          creationDateBefore,
          creationDateBefore,
          1,
          0,
          "",
          "",
          "",
          "",
          nullWorkspaceId);

  private RunSet runSetCreatedBackfilled =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000004"),
          methodVersionCreatedBackfilled,
          "RunSetCreatedBackfilled",
          "A run set that was created in the current workspace, with a backfilled originalWorkspaceId",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          creationDateAfter,
          creationDateAfter,
          creationDateAfter,
          1,
          0,
          "",
          "",
          "",
          "",
          nullWorkspaceId);

  private RunSet templateClonedNull =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000011"),
          methodVersionClonedNull,
          "TemplateClonedNull",
          "A run set that was cloned from a source workspace, with a null originalWorkspaceId",
          false,
          true,
          CbasRunSetStatus.COMPLETE,
          creationDateBefore,
          creationDateBefore,
          creationDateBefore,
          1,
          0,
          "",
          "",
          "",
          "",
          null);

  private RunSet templateCreatedNull =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000022"),
          methodVersionCreatedNull,
          "TemplateCreatedNull",
          "A run set that was created in the current workspace, with a null originalWorkspaceId",
          false,
          true,
          CbasRunSetStatus.COMPLETE,
          creationDateAfter,
          creationDateAfter,
          creationDateAfter,
          1,
          0,
          "",
          "",
          "",
          "",
          null);

  private RunSet templateClonedBackfilled =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000033"),
          methodVersionClonedBackfilled,
          "TemplateClonedBackfilled",
          "A run set that was cloned from a source workspace, with a backfilled originalWorkspaceId",
          false,
          true,
          CbasRunSetStatus.COMPLETE,
          creationDateBefore,
          creationDateBefore,
          creationDateBefore,
          1,
          0,
          "",
          "",
          "",
          "",
          nullWorkspaceId);

  private RunSet templateCreatedBackfilled =
      new RunSet(
          UUID.fromString("00000000-0000-0000-0000-000000000044"),
          methodVersionCreatedBackfilled,
          "TemplateCreatedBackfilled",
          "A run set that was created in the current workspace, with a backfilled originalWorkspaceId",
          false,
          true,
          CbasRunSetStatus.COMPLETE,
          creationDateAfter,
          creationDateAfter,
          creationDateAfter,
          1,
          0,
          "",
          "",
          "",
          "",
          nullWorkspaceId);
}
