package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.util.BackfillOriginalWorkspaceIds;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

    // NOTE:  The following BackfillOriginalWorkspaceIds method calls are inherently temporary.
    //        Once all original workspace IDs have been backfilled, these lines
    //        (and the BackfillOriginalWorkspaceIds class itself) should be deleted.
    BackfillOriginalWorkspaceIds.backfillRunSets(runSetDao, cbasContextConfig, logger);
    BackfillOriginalWorkspaceIds.backfillMethods(methodDao, cbasContextConfig, logger);
    BackfillOriginalWorkspaceIds.backfillMethodVersions(
        methodVersionDao, cbasContextConfig, logger);

    // BackfillOriginalWorkspaceIds method calls should precede pruneOriginalWorkspaceHistory
    // until all workspaces have been backfilled.
    pruneOriginalWorkspaceHistory();
  }

  public List<UUID> getMethodLatestRunSetIds() {
    return methodDao.getMethods().stream()
        .map(
            m -> {
              if (m.lastRunSetId() != null) {
                return m.lastRunSetId();
              } else {
                // TODO: does getRunSetWithMethodId retrieve templates as well?
                return runSetDao.getRunSetWithMethodId(m.methodId()).runSetId();
              }
            })
        .collect(Collectors.toList());
  }

  public void pruneOriginalWorkspaceHistory() {
    runDao.deleteRunsBefore(cbasContextConfig.getWorkspaceCreatedDate());
    logger.info("Deleted all runs prior to workspace creation date");

    List<UUID> latestRunSetIdsPerMethod = getMethodLatestRunSetIds();
    logger.info("Identified latest run set per method: {}", latestRunSetIdsPerMethod);

    // prune all run sets except the latest for each method, and set them as templates
    runSetDao.deleteAllExcept(latestRunSetIdsPerMethod);
    latestRunSetIdsPerMethod.forEach(rsId -> runSetDao.updateIsTemplate(rsId, true));
  }
}
