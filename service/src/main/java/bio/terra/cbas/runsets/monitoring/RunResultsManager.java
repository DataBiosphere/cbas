package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.exceptions.RunResultsInvalidRunIdException;
import bio.terra.cbas.runsets.exceptions.RunResultsUpdateErrorException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RunResultsManager {
  private final RunDao runDao;
  private final SmartRunsPoller smartRunsPoller;
  private final CromwellService cromwellService;
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RunResultsManager.class);

  public RunResultsManager(
      RunSetDao runSetDao,
      RunDao runDao,
      SmartRunsPoller smartRunsPoller,
      CromwellService cromwellService) {
    this.runDao = runDao;
    this.smartRunsPoller = smartRunsPoller;
    this.cromwellService = cromwellService;
  }

  private Run getRunRecordById(UUID runId) {
    Optional<Run> runRecord =
        runDao.getRuns(new RunDao.RunsFilters(runId, null)).stream().findFirst();
    if (runRecord.isPresent()) {
      return runRecord.get();
    }
    throw new RunResultsInvalidRunIdException(
        String.format("Results can not be posted for a non-existing workflow ID {}.", runId));
  }

  public void updateResults(UUID runId, CbasRunStatus updatedRunState, Object outputs) {
    long startTimeNs = System.nanoTime();
    boolean successBoolean = false;

    try {
      // lookup runID in database
      Run updatableRun = getRunRecordById(runId);
      // if status is complete, updates outputs if any
      if (updatedRunState == CbasRunStatus.COMPLETE && outputs != null) {
        try {
          // we only write back output attributes to WDS if output definition is not empty.
          if (smartRunsPoller.hasOutputDefinition(updatableRun)) {
            smartRunsPoller.updateOutputAttributes(updatableRun, outputs);
          } else {
            // when workflow outputs do not match the workflow definition, log a warning.
            logger.warn(
                "Workflow ID {} has no outputs defined, skipped writing back output posted {}.",
                updatableRun.runId(),
                outputs);
          }
        } catch (Exception e) {
          // log error and mark Run as Failed
          String errorMessage =
              "Error while updating data table attributes for record %s from run %s (engine workflow ID %s): %s"
                  .formatted(
                      updatableRun.recordId(),
                      updatableRun.runId(),
                      updatableRun.engineId(),
                      e.getMessage());
          logger.error(errorMessage, e);
          throw new RunResultsUpdateErrorException(
              "Error while updating data table attributes for engine workflow ID %s): %s"
                  .formatted(updatableRun.runId(), e.getMessage()));
        }
      }
      // update status
      int changes;
      changes = runDao.updateRunStatus(updatableRun.runId(), updatedRunState, OffsetDateTime.now());
      successBoolean = true;
      logger.info("Updating status of workflow ID to {}.", runId, updatedRunState);
      if (changes == 1) {
        logger.warn(
            "Run {} was identified for updating status from {} to {} but no DB rows were changed by the query.",
            updatableRun.runId(),
            updatableRun.status(),
            updatedRunState);
      }
    } finally {
      recordMethodCompletion(startTimeNs, successBoolean);
    }
  }
}
