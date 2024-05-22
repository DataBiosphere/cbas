package bio.terra.cbas.service;

import bio.terra.cbas.dao.GithubMethodDetailsDao;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import java.util.List;
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

  public void deleteMethod(UUID methodId) {

    // TODO: put this in RunSetService
    runSetDao
        .getRunSetsWithMethodId(methodId)
        .forEach(
            runSet -> {
              List<Run> runSetRuns =
                  runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), null));
              runSetRuns.forEach(run -> runDao.deleteRun(run.runId()));
              runSetDao.deleteRunSet(runSet.runSetId());
            });

    Method methodToDelete = methodDao.getMethod(methodId);
    methodVersionDao
        .getMethodVersionsForMethod(methodToDelete)
        .forEach(
            methodVersion -> methodVersionDao.deleteMethodVersion(methodVersion.methodVersionId()));

    githubMethodDetailsDao.deleteMethodSourceDetails(methodId);

    methodDao.deleteMethod(methodId);
  }
}
