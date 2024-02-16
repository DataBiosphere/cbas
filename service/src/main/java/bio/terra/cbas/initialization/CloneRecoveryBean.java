package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

    backfillRunSetOriginalWorkspaceIds();
    backfillMethodOriginalWorkspaceIds();
    backfillMethodVersionOriginalWorkspaceIds();
  }

  public List<RunSet> backfillRunSetOriginalWorkspaceIds() {
    List<RunSet> runSetsToBackfill =
        runSetDao.getRunSets(null, false).stream()
            .filter(rs -> rs.getOriginalWorkspaceId() == null)
            .toList();
    logger.info("{} Run Sets have null originalWorkspaceIds.", runSetsToBackfill.size());

    if (!runSetsToBackfill.isEmpty()) {
      logger.info("Back-filling {} run sets", runSetsToBackfill.size());
      runSetsToBackfill.stream()
          .forEach(
              rs -> {
                UUID updatedOriginalWorkspaceUUID;
                if (rs.submissionTimestamp().isAfter(cbasContextConfig.getWorkspaceCreatedDate())) {
                  // if the run set was submitted after the workspace was created, we know it wasn't
                  // cloned.
                  updatedOriginalWorkspaceUUID = cbasContextConfig.getWorkspaceId();
                } else {
                  // otherwise, we know the run set *was* cloned, but we don't know from which
                  // workspace.
                  // All-zero UUID is our convention for "we don't know where this came from".
                  updatedOriginalWorkspaceUUID =
                      UUID.fromString("00000000-0000-0000-0000-000000000000");
                }
                runSetDao.updateOriginalWorkspaceId(rs.runSetId(), updatedOriginalWorkspaceUUID);
              });
    }
    return runSetsToBackfill;
  }

  public List<Method> backfillMethodOriginalWorkspaceIds() {
    List<Method> methodsToBackfill =
        methodDao.getMethods().stream()
            .filter(m -> m.getOriginalWorkspaceId() == null)
            .collect(Collectors.toList());

    logger.info("{} Methods have null originalWorkspaceIds.", methodsToBackfill.size());

    if (!methodsToBackfill.isEmpty()) {
      logger.info("Back-filling {} Methods", methodsToBackfill.size());
      methodsToBackfill.stream()
          .forEach(
              m -> {
                UUID updatedOriginalWorkspaceUUID;
                if (m.created().isAfter(cbasContextConfig.getWorkspaceCreatedDate())) {
                  // if the method was created after the workspace was created, we know it wasn't
                  // cloned.
                  updatedOriginalWorkspaceUUID = cbasContextConfig.getWorkspaceId();
                } else {
                  // otherwise, we know the method *was* cloned, but we don't know from which
                  // workspace.
                  // All-zero UUID is our convention for "we don't know where this came from".
                  updatedOriginalWorkspaceUUID =
                      UUID.fromString("00000000-0000-0000-0000-000000000000");
                }
                logger.info(
                    "Updating method {} original workspace id to {}",
                    m.methodId(),
                    updatedOriginalWorkspaceUUID);

                methodDao.updateOriginalWorkspaceId(m.methodId(), updatedOriginalWorkspaceUUID);
              });
    }

    return methodsToBackfill;
  }

  public List<MethodVersion> backfillMethodVersionOriginalWorkspaceIds() {
    List<MethodVersion> methodVersionsToBackfill =
        methodVersionDao.getMethodVersions().stream()
            .filter(mv -> mv.getOriginalWorkspaceId() == null)
            .collect(Collectors.toList());

    logger.info(
        "{} Method Versions have null originalWorkspaceIds.", methodVersionsToBackfill.size());

    if (!methodVersionsToBackfill.isEmpty()) {
      logger.info("Back-filling {} Method Versions", methodVersionsToBackfill.size());
      methodVersionsToBackfill.stream()
          .filter(mv -> mv.getOriginalWorkspaceId() == null)
          .forEach(
              mv -> {
                UUID updatedOriginalWorkspaceUUID;
                if (mv.created().isAfter(cbasContextConfig.getWorkspaceCreatedDate())) {
                  // if the method version was created after the workspace was created, we know it
                  // wasn't cloned.
                  updatedOriginalWorkspaceUUID = cbasContextConfig.getWorkspaceId();
                } else {
                  // otherwise, we know the method version *was* cloned, but we don't know from
                  // which workspace.
                  // All-zero UUID is our convention for "we don't know where this came from".
                  updatedOriginalWorkspaceUUID =
                      UUID.fromString("00000000-0000-0000-0000-000000000000");
                }
                methodVersionDao.updateOriginalWorkspaceId(
                    mv.methodVersionId(), updatedOriginalWorkspaceUUID);
              });
    }
    return methodVersionsToBackfill;
  }
}
