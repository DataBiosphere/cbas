package bio.terra.cbas.service;

import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.model.MethodLastRunDetails;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MethodService {
  private final MethodDao methodDao;

  public MethodService(MethodDao methodDao) {
    this.methodDao = methodDao;
  }

  public void archiveMethod(UUID methodId) {
    methodDao.archiveMethod(methodId);
  }

  public static MethodLastRunDetails initializeLastRunDetails(UUID lastRunSetId) {
    MethodLastRunDetails lastRunDetails = new MethodLastRunDetails();
    if (lastRunSetId != null) {
      lastRunDetails.setRunSetId(lastRunSetId);
      lastRunDetails.setPreviouslyRun(true);
    } else {
      lastRunDetails.setPreviouslyRun(false);
    }
    return lastRunDetails;
  }
}
