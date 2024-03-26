package bio.terra.cbas.initialization;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.initialization.cloneRecovery.CloneRecoveryTransactionService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCloneRecoveryTransactionService {
  RunDao runDao;
  RunSetDao runSetDao;
  CloneRecoveryTransactionService transactionService;

  @BeforeEach
  void setup() {
    runSetDao = mock(RunSetDao.class);
    runDao = mock(RunDao.class);
    when(runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), null))).thenReturn(List.of(run));
    transactionService = new CloneRecoveryTransactionService(runSetDao, runDao);
  }

  @Test
  void convertToTemplate() {
    transactionService.convertToTemplate(runSet);
    verify(runSetDao).updateIsTemplate(runSet.runSetId(), true);
    verify(runDao).deleteRun(run.runId());
  }

  @Test
  void deleteRunSetsAndRuns() {
    transactionService.deleteRunSetAndRuns(runSet);
    verify(runSetDao).deleteRunSet(runSet.runSetId());
    verify(runDao).deleteRun(run.runId());
  }

  RunSet runSet =
      new RunSet(
          UUID.randomUUID(),
          null,
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

  Run run =
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
