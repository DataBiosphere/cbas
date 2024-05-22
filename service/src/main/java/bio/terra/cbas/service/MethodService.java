package bio.terra.cbas.service;

import bio.terra.cbas.dao.GithubMethodDetailsDao;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MethodService {
  private final MethodDao methodDao;
  private final MethodVersionDao methodVersionDao;
  private final RunSetDao runSetDao;
  private final RunDao runDao;
  private final GithubMethodDetailsDao githubMethodDetailsDao;

  public MethodService(
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      RunSetDao runSetDao,
      RunDao runDao,
      GithubMethodDetailsDao githubMethodDetailsDao) {
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.githubMethodDetailsDao = githubMethodDetailsDao;
  }

  public void archiveMethod(UUID methodId) {
    methodDao.archiveMethod(methodId);
  }
}
