package bio.terra.cbas.util;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/*
NOTE: This class is inherently temporary.  Once all original workspace IDs have been backfilled,
      this entire class should be deleted, along with its method calls in BackfillOriginalWorkspaceIdBean.java
 */
@Service
public class BackfillOriginalWorkspaceIdService {
  private final Logger logger = LoggerFactory.getLogger(BackfillOriginalWorkspaceIdService.class);
  private final CbasContextConfiguration cbasContextConfig;
  private final RunSetDao runSetDao;
  private final MethodDao methodDao;
  private final MethodVersionDao methodVersionDao;

  public BackfillOriginalWorkspaceIdService(
      CbasContextConfiguration cbasContextConfig,
      RunSetDao runSetDao,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao) {
    this.cbasContextConfig = cbasContextConfig;
    this.methodDao = methodDao;
    this.runSetDao = runSetDao;
    this.methodVersionDao = methodVersionDao;
  }

  public void backfillAll() {
    try {
      backfillMethods();
      backfillRunSets();
      backfillMethodVersions();
    } catch (Exception e) {
      // we want CBAS to fail if this operation fails, just like a liquibase migration
      throw new RuntimeException(
          "There was a problem while trying to backfill original workspace IDs: %s".formatted(e));
    }
  }

  public List<RunSet> getRunSetsToBackfill() {
    return runSetDao.getRunSets(null, false).stream()
        .filter(rs -> rs.getOriginalWorkspaceId() == null)
        .toList();
  }

  public List<UUID> backfillRunSets() {
    List<RunSet> runSetsToBackfill = getRunSetsToBackfill();
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
                logger.info(
                    "Updating runSet {} original workspace id to {}",
                    rs.runSetId(),
                    updatedOriginalWorkspaceUUID);
                runSetDao.updateOriginalWorkspaceId(rs.runSetId(), updatedOriginalWorkspaceUUID);
              });
    }
    return runSetsToBackfill.stream().map(RunSet::runSetId).toList();
  }

  public List<Method> getMethodsToBackfill() {
    return methodDao.getMethods().stream()
        .filter(m -> m.getOriginalWorkspaceId() == null)
        .collect(Collectors.toList());
  }

  public List<UUID> backfillMethods() {
    List<Method> methodsToBackfill = getMethodsToBackfill();

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

    return methodsToBackfill.stream().map(Method::methodId).toList();
  }

  public List<MethodVersion> getMethodVersionsToBackfill() {
    return methodVersionDao.getMethodVersions().stream()
        .filter(mv -> mv.getOriginalWorkspaceId() == null)
        .collect(Collectors.toList());
  }

  public List<UUID> backfillMethodVersions() {
    List<MethodVersion> methodVersionsToBackfill = getMethodVersionsToBackfill();

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
                logger.info(
                    "Updating methodVersion {} original workspace id to {}",
                    mv.methodVersionId(),
                    updatedOriginalWorkspaceUUID);

                methodVersionDao.updateOriginalWorkspaceId(
                    mv.methodVersionId(), updatedOriginalWorkspaceUUID);
              });
    }
    return methodVersionsToBackfill.stream().map(MethodVersion::methodVersionId).toList();
  }
}
