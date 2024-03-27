package bio.terra.cbas.initialization.cloneRecovery;

import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.RunSet;
import bio.terra.common.db.WriteTransaction;
import org.springframework.stereotype.Service;

@Service
public class CloneRecoveryTransactionService {
  private final RunDao runDao;
  private final RunSetDao runSetDao;
  private final MethodDao methodDao;
  private final MethodVersionDao methodVersionDao;

  public CloneRecoveryTransactionService(
      RunSetDao runSetDao, RunDao runDao, MethodDao methodDao, MethodVersionDao methodVersionDao) {
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
  }

  private void deleteRuns(RunSet runSet) {
    runDao
        .getRuns(new RunDao.RunsFilters(runSet.runSetId(), null))
        .forEach(r -> runDao.deleteRun(r.runId()));
  }

  private void unsetLastRunSet(RunSet runSet) {
    methodDao.unsetLastRunSetId(runSet.methodVersion().getMethodId());
    methodVersionDao.unsetLastRunSetId(runSet.getMethodVersionId());
  }

  @WriteTransaction
  public void convertToTemplate(RunSet runSet) {
    deleteRuns(runSet);
    runSetDao.updateIsTemplate(runSet.runSetId(), true);
    unsetLastRunSet(runSet);
  }

  @WriteTransaction
  public void deleteRunSetAndRuns(RunSet runSet) {
    deleteRuns(runSet);
    runSetDao.deleteRunSet(runSet.runSetId());
    unsetLastRunSet(runSet);
  }
}
