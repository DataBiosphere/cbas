package bio.terra.cbas.initialization.cloneRecovery;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.RunSet;
import bio.terra.common.db.WriteTransaction;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CloneRecoveryTransactionService {
  private final RunDao runDao;
  private final RunSetDao runSetDao;

  public CloneRecoveryTransactionService(RunSetDao runSetDao, RunDao runDao) {
    this.runSetDao = runSetDao;
    this.runDao = runDao;
  }

  @WriteTransaction
  public void deleteRuns(UUID runSetId) {
    runDao
        .getRuns(new RunDao.RunsFilters(runSetId, null))
        .forEach(r -> runDao.deleteRun(r.runId()));
  }

  @WriteTransaction
  public void convertToTemplate(RunSet runSet) {
    deleteRuns(runSet.runSetId());
    runSetDao.updateIsTemplate(runSet.runSetId(), true);
  }

  @WriteTransaction
  public void deleteRunSetAndRuns(RunSet runSet) {
    deleteRuns(runSet.runSetId());
    runSetDao.deleteRunSet(runSet.runSetId());
  }
}
