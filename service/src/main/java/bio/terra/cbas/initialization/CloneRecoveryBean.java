package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.util.BackfillOriginalWorkspaceIds;
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
        "Starting clone recovery (workspaceId: {}, workspaceCreatedDate: {}",
        cbasContextConfig.getWorkspaceId(),
        cbasContextConfig.getWorkspaceCreatedDate());

    // TODO: Should we be deleting all runs at this point?

    BackfillOriginalWorkspaceIds.backfillRunSets(runSetDao, cbasContextConfig, logger);
    BackfillOriginalWorkspaceIds.backfillMethods(methodDao, cbasContextConfig, logger);
    BackfillOriginalWorkspaceIds.backfillMethodVersions(
        methodVersionDao, cbasContextConfig, logger);
  }
}
