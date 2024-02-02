package bio.terra.cbas.initialization;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloneRecoveryBean {

  private final Logger logger = LoggerFactory.getLogger(CloneRecoveryBean.class);

  private final RunDao runDao;
  private final RunSetDao runSetDao;

  public CloneRecoveryBean(RunSetDao runSetDao, RunDao runDao) {
    this.runSetDao = runSetDao;
    this.runDao = runDao;
  }

  public void cloneRecovery() {
    logger.info("Starting clone recovery");
  }
}
