package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.common.db.WriteTransaction;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CloneRecoveryService {

  private final Logger logger = LoggerFactory.getLogger(CloneRecoveryService.class);
  private final RunDao runDao;
  private final RunSetDao runSetDao;
  private final MethodDao methodDao;
  private final CbasContextConfiguration cbasContextConfig;

  public CloneRecoveryService(
      RunSetDao runSetDao,
      RunDao runDao,
      MethodDao methodDao,
      CbasContextConfiguration cbasContextConfig) {
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.methodDao = methodDao;
    this.cbasContextConfig = cbasContextConfig;
  }

  public void cloneRecovery() {
    logger.info(
        "Starting clone recovery (workspaceId: {}, workspaceCreatedDate: {})",
        cbasContextConfig.getWorkspaceId(),
        cbasContextConfig.getWorkspaceCreatedDate());

    List<Method> clonedMethods =
        methodDao.getMethods().stream().filter(this::isMethodCloned).toList();

    List<MethodTemplateUpdateManifest> templateUpdateManifests =
        clonedMethods.stream()
            .map(m -> runSetDao.getRunSetsWithMethodId(m.methodId()))
            .map(this::generateTemplateUpdateManifest)
            .toList();

    templateUpdateManifests.forEach(
        manifest -> {
          manifest.templateRunSets.forEach(rs -> runSetDao.updateIsTemplate(rs.runSetId(), true));
          manifest.nonTemplateRunSets.forEach(
              rs -> runSetDao.updateIsTemplate(rs.runSetId(), false));
        });

    clonedMethods.forEach(this::cleanupClonedMethodHistory);
  }

  public record MethodTemplateUpdateManifest(
      List<RunSet> templateRunSets, List<RunSet> nonTemplateRunSets) {}
  ;

  public MethodTemplateUpdateManifest generateTemplateUpdateManifest(List<RunSet> methodRunSets) {
    List<RunSet> methodClonedRunSets = methodRunSets.stream().filter(this::isRunSetCloned).toList();

    if (methodClonedRunSets.isEmpty()) {
      return new MethodTemplateUpdateManifest(Collections.emptyList(), Collections.emptyList());
    } else {
      return new MethodTemplateUpdateManifest(
          List.of(methodClonedRunSets.get(0)), methodClonedRunSets.stream().skip(1).toList());
    }
  }

  public void cleanupClonedMethodHistory(Method method) {
    runSetDao.getRunSetsWithMethodId(method.methodId()).stream()
        .filter(this::isRunSetCloned)
        .forEach(this::deleteRunSetRuns);
  }

  @WriteTransaction
  public void deleteRunSetRuns(RunSet runSet) {
    List<Run> runsToDelete = runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), null));
    runsToDelete.forEach(r -> runDao.deleteRun(r.runId()));
    if (!runSet.isTemplate()) {
      runSetDao.deleteRunSet(runSet.runSetId());
    }
  }

  public Boolean isMethodCloned(Method m) {
    return !m.originalWorkspaceId().equals(cbasContextConfig.getWorkspaceId());
  }

  public Boolean isRunSetCloned(RunSet r) {
    return !r.originalWorkspaceId().equals(cbasContextConfig.getWorkspaceId());
  }
}
