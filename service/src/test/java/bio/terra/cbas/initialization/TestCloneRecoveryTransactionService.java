package bio.terra.cbas.initialization;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.initialization.cloneRecovery.CloneRecoveryTransactionService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCloneRecoveryTransactionService {
  RunDao runDao;
  RunSetDao runSetDao;
  MethodDao methodDao;
  MethodVersionDao methodVersionDao;
  CloneRecoveryTransactionService transactionService;

  @BeforeEach
  void setup() {
    runSetDao = mock(RunSetDao.class);
    runDao = mock(RunDao.class);
    methodDao = mock(MethodDao.class);
    methodVersionDao = mock(MethodVersionDao.class);
    when(runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), null)))
        .thenReturn(List.of(run1, run2, run3));
    transactionService =
        new CloneRecoveryTransactionService(runSetDao, runDao, methodDao, methodVersionDao);
  }

  @Test
  void convertToTemplate() {
    transactionService.convertToTemplate(runSet);
    verify(runSetDao).updateIsTemplate(runSet.runSetId(), true);
    verify(runDao).deleteRun(run1.runId());
    verify(runDao).deleteRun(run2.runId());
    verify(runDao).deleteRun(run3.runId());
  }

  @Test
  void deleteRunSetsAndRuns() {
    transactionService.deleteRunSetAndRuns(runSet);
    verify(runSetDao).deleteRunSet(runSet.runSetId());
    verify(runDao).deleteRun(run1.runId());
    verify(runDao).deleteRun(run2.runId());
    verify(runDao).deleteRun(run3.runId());
    verify(methodDao).unsetLastRunSetId(method.methodId());
    verify(methodVersionDao).unsetLastRunSetId(methodVersion.methodVersionId());
  }

  Method method = new Method(UUID.randomUUID(), "", "", null, null, "", null);
  MethodVersion methodVersion =
      new MethodVersion(UUID.randomUUID(), method, "", "", null, null, "", null, "");

  RunSet runSet =
      new RunSet(
          UUID.randomUUID(),
          methodVersion,
          "clonedRunSet",
          "",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          null,
          null,
          null,
          0,
          0,
          "[]",
          "[]",
          "",
          "",
          null);

  Run run1 =
      new Run(
          UUID.randomUUID(),
          UUID.randomUUID().toString(),
          runSet,
          "",
          runSet.submissionTimestamp(),
          CbasRunStatus.COMPLETE,
          runSet.lastModifiedTimestamp(),
          runSet.lastPolledTimestamp(),
          "");

  Run run2 =
      new Run(
          UUID.randomUUID(),
          UUID.randomUUID().toString(),
          runSet,
          "",
          runSet.submissionTimestamp(),
          CbasRunStatus.COMPLETE,
          runSet.lastModifiedTimestamp(),
          runSet.lastPolledTimestamp(),
          "");

  Run run3 =
      new Run(
          UUID.randomUUID(),
          UUID.randomUUID().toString(),
          runSet,
          "",
          runSet.submissionTimestamp(),
          CbasRunStatus.COMPLETE,
          runSet.lastModifiedTimestamp(),
          runSet.lastPolledTimestamp(),
          "");
}
