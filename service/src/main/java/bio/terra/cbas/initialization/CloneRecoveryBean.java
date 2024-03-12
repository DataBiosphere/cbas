package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.util.BackfillOriginalWorkspaceIds;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    runDao.deleteRunsBefore(cbasContextConfig.getWorkspaceCreatedDate());
    logger.info("Deleted all runs prior to workspace creation date");

    Set<UUID> latestRunSetIdsPerMethod =
        methodDao.getMethods().stream()
            .map(
                m -> {
                  if (m.lastRunSetId() != null) {
                    return m.lastRunSetId();
                  } else {
                    return runSetDao
                        .getRunSetWithMethodId(m.methodId())
                        .runSetId(); // TODO: is this getting the latest or earliest run set for the
                    // method?
                  }
                })
            .collect(Collectors.toSet());
    logger.info("Identified latest run set per method: {}", latestRunSetIdsPerMethod);

    // prune all run sets except the latest for each method, and set them as templates
    Stream<RunSet> runSets =
        runSetDao.getRunSets(null, false).stream()
            .filter(
                rs ->
                    rs.submissionTimestamp().isBefore(cbasContextConfig.getWorkspaceCreatedDate()));
    Stream<RunSet> templates = runSetDao.getRunSets(null, true).stream();
    Stream.concat(runSets, templates)
        .forEach(
            rs -> {
              if (!latestRunSetIdsPerMethod.contains(rs.runSetId())) {
                logger.info("Deleting run set {}", rs.runSetId());
                runSetDao.deleteRunSet(rs.runSetId());
              } else if (!rs.isTemplate()) {
                logger.info("Converting run set {} to template.", rs.runSetId());
                runSetDao.updateIsTemplate(rs.runSetId(), true);
              } else {
                logger.info("Run set {} is already a template; no change.", rs.runSetId());
              }
            });

    //    logger.info("setting lastRunSetId for methods and method versions to null");
    //    methodDao.getMethods().stream().forEach(m -> methodDao.updateLastRunSetId(null,
    // m.methodId()));
    //    methodVersionDao.getMethodVersions().stream()
    //        .forEach(mv -> methodDao.updateLastRunSetId(null, mv.methodVersionId()));
  }
}
