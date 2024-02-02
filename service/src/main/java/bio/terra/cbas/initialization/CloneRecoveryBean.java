package bio.terra.cbas.initialization;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;

public class CloneRecoveryBean {

  private final RunDao runDao;
  private final RunSetDao runSetDao;

  public CloneRecoveryBean(RunSetDao runSetDao, RunDao runDao) {
    this.runSetDao = runSetDao;
    this.runDao = runDao;
  }

  public void cloneRecovery() {
    System.out.println("Running clone recovery function!");
  }
}
