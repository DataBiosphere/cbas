package bio.terra.cbas.service;

import bio.terra.cbas.dao.MethodDao;
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
}
