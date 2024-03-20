package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.common.db.WriteTransaction;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CloneRecoveryService {

  private final Logger logger = LoggerFactory.getLogger(CloneRecoveryService.class);
  @Autowired private final RunDao runDao;
  @Autowired private final RunSetDao runSetDao;
  @Autowired private final MethodDao methodDao;
  @Autowired private final CbasContextConfiguration cbasContextConfig;

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

    pruneCloneSourceWorkspaceHistory();
  }

  @WriteTransaction
  public void pruneCloneSourceWorkspaceHistory() {
    List<Method> methods = methodDao.getMethods().stream().filter(this::isMethodCloned).toList();

    methods.forEach(this::updateMethodTemplate);

    methods.forEach(this::cleanupClonedMethodHistory);
  }

  public Boolean isMethodCloned(Method m) {
    return !m.originalWorkspaceId().equals(cbasContextConfig.getWorkspaceId());
  }

  public Boolean isRunSetCloned(RunSet r) {
    return !r.originalWorkspaceId().equals(cbasContextConfig.getWorkspaceId());
  }

  @WriteTransaction
  public void updateMethodTemplate(Method m) {
    Stream<RunSet> runSetsAndTemplates =
        Stream.concat(
            runSetDao.getRunSets(null, false).stream(), runSetDao.getRunSets(null, true).stream());

    List<RunSet> methodClonedRunSets =
        runSetsAndTemplates
            .filter(this::isRunSetCloned)
            .filter(rs -> rs.methodVersion().getMethodId().equals(m.methodId()))
            .sorted(Comparator.comparing(RunSet::submissionTimestamp).reversed())
            .toList();

    if (!methodClonedRunSets.isEmpty()) {
      RunSet newTemplateRunSet = methodClonedRunSets.get(0);
      runSetDao.updateIsTemplate(newTemplateRunSet.runSetId(), true);
      methodClonedRunSets.stream()
          .skip(1) // skip the latest run set, which was promoted to isTemplate = true
          .forEach(rs -> runSetDao.updateIsTemplate(rs.runSetId(), false));
    }
  }

  @WriteTransaction
  public void cleanupClonedMethodHistory(Method method) {
    runSetDao.getRunSetsWithMethodId(method.methodId()).stream()
        .filter(this::isRunSetCloned)
        .forEach(
            rs -> {
              deleteRunSetRuns(rs);
              if (!rs.isTemplate()) {
                runSetDao.deleteRunSet(rs.runSetId());
              }
            });
  }

  @WriteTransaction
  public void deleteRunSetRuns(RunSet runSet) {
    List<Run> runsToDelete = runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), null));
    runsToDelete.forEach(r -> runDao.deleteRun(r.runId()));
  }
}
