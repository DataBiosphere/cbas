package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloneRecoveryBean {

  private final Logger logger = LoggerFactory.getLogger(CloneRecoveryBean.class);
  private final RunDao runDao;
  private final RunSetDao runSetDao;
  private final MethodDao methodDao;
  private final MethodVersionDao methodVersionDao;
  private final CbasContextConfiguration cbasContextConfig;

  public CloneRecoveryBean(
      RunSetDao runSetDao,
      RunDao runDao,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      CbasContextConfiguration cbasContextConfig) {
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
    this.cbasContextConfig = cbasContextConfig;
  }

  public void cloneRecovery() {
    logger.info(
        "Starting clone recovery (workspaceId: {}, workspaceCreatedDate: {})",
        cbasContextConfig.getWorkspaceId(),
        cbasContextConfig.getWorkspaceCreatedDate());

    pruneCloneSourceWorkspaceHistory();
  }

  public void pruneCloneSourceWorkspaceHistory() {
    methodDao.getMethods().stream()
        .filter(this::isMethodCloned)
        .forEach(this::updateMethodTemplate);

    runSetDao.getRunSets(null, true).forEach(this::deleteRunSetRuns);

    runSetDao
        .getRunSets(null, false)
        .forEach(
            rs -> {
              deleteRunSetRuns(rs);
              runSetDao.deleteRunSet(rs.runSetId());
            });
  }

  public Boolean isMethodCloned(Method m) {
    return m.originalWorkspaceId() != cbasContextConfig.getWorkspaceId();
  }

  public UUID updateMethodTemplate(Method m) {
    UUID newTemplateRunSetId;
    if (m.lastRunSetId() != null) {
      newTemplateRunSetId = m.lastRunSetId();
    } else {
      newTemplateRunSetId = runSetDao.getLatestRunSetWithMethodId(m.methodId()).runSetId();
    }
    runSetDao
        .getRunSetsWithMethodId(m.methodId())
        .forEach(rs -> runSetDao.updateIsTemplate(rs.runSetId(), false));
    runSetDao.updateIsTemplate(newTemplateRunSetId, true);
    return newTemplateRunSetId;
  }

  public void deleteRunSetRuns(RunSet runSet) {
    logger.info("deleteRunSetRuns: {}", runSet.runSetId());
    List<Run> runsToDelete = runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), null));
    runsToDelete.forEach(r -> runDao.deleteRun(r.runId()));
  }
}
