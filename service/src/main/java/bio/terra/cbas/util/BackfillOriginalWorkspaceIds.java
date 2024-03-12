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
/*
NOTE: This class is inherently temporary.  Once all original workspace IDs have been backfilled,
      this entire class should be deleted, along with its method calls in CloneRecoveryBean.java
 */
public class BackfillOriginalWorkspaceIds {

  public static List<RunSet> backfillRunSets(
      RunSetDao runSetDao, CbasContextConfiguration cbasContextConfig, Logger logger) {
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
                logger.info(
                    "Updating runSet {} original workspace id to {}",
                    rs.runSetId(),
                    updatedOriginalWorkspaceUUID);
                runSetDao.updateOriginalWorkspaceId(rs.runSetId(), updatedOriginalWorkspaceUUID);
              });
    }
    return runSetsToBackfill;
  }

  public static List<Method> backfillMethods(
      MethodDao methodDao, CbasContextConfiguration cbasContextConfig, Logger logger) {
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

  public static List<MethodVersion> backfillMethodVersions(
      MethodVersionDao methodVersionDao,
      CbasContextConfiguration cbasContextConfig,
      Logger logger) {
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
                logger.info(
                    "Updating methodVersion {} original workspace id to {}",
                    mv.methodVersionId(),
                    updatedOriginalWorkspaceUUID);

                methodVersionDao.updateOriginalWorkspaceId(
                    mv.methodVersionId(), updatedOriginalWorkspaceUUID);
              });
    }
    return methodVersionsToBackfill;
  }
}
