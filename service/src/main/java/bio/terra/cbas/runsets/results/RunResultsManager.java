package bio.terra.cbas.runsets.results;

import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

  public RunResultsUpdateResponse updateResults(
      Run updatableRun, CbasRunStatus status, Object outputs) {
    OffsetDateTime engineChangedTimestamp = DateUtils.currentTimeInUTC();
    ;
    long updateResultsStartNanos = System.nanoTime();
    boolean updateResultsSuccess = false;

    try {
      var updatedRunState = status;
      if (updatableRun.status() != updatedRunState) {
        ArrayList<String> errors = new ArrayList<>();

        if (updatedRunState == CbasRunStatus.COMPLETE) {
          String errorMessage = saveRunOutputs(updatableRun, outputs);
          if (errorMessage != null && !errorMessage.isEmpty()) {
            errors.add(errorMessage);
            updatedRunState = CbasRunStatus.SYSTEM_ERROR;
          }
        } else if (updatedRunState.inErrorState()) {
          var cromwellErrors = getRunErrors(updatableRun);
          if (!cromwellErrors.isEmpty()) {
            errors.addAll(cromwellErrors);
          }
        }
        // Save the updated run record in database
        logger.info(
            "Updating status of Run {} (engine ID {}) from {} to {} with {} errors",
            updatableRun.runId(),
            updatableRun.engineId(),
            updatableRun.status(),
            updatedRunState,
            errors.size());
        int changes;
        if (errors.isEmpty()) {
          changes =
              runDao.updateRunStatus(updatableRun.runId(), updatedRunState, engineChangedTimestamp);
        } else {
          updatableRun = updatableRun.withErrorMessages(String.join(", ", errors));
          changes =
              runDao.updateRunStatusWithError(
                  updatableRun.runId(),
                  updatedRunState,
                  engineChangedTimestamp,
                  updatableRun.errorMessages());
        }
        if (changes == 1) {
          updatableRun =
              updatableRun
                  .withStatus(updatedRunState)
                  .withLastModified(engineChangedTimestamp)
                  .withLastPolled(OffsetDateTime.now());
        } else {
          logger.warn(
              "Run {} was identified for updating status from {} to {} but no DB rows were changed by the query.",
              updatableRun.runId(),
              updatableRun.status(),
              updatedRunState);
        }
      } else {
        // if run status hasn't changed, only update last polled(modified) timestamp
        var changes = runDao.updateLastPolledTimestamp(updatableRun.runId());
        if (changes != 1) {
          logger.warn(
              "Expected 1 row change updating last_polled_timestamp for Run {} in status {}, but got {}.",
              updatableRun.runId(),
              updatableRun.status(),
              changes);
        }
      }
    } finally {
      recordMethodCompletion(updateResultsStartNanos, updateResultsSuccess);
    }
    return new RunResultsUpdateResponse(updateResultsSuccess, updatableRun.errorMessages());
  }

  private String saveRunOutputs(Run updatableRun, Object outputs) {
    try {
      // we only write back output attributes to WDS if output definition is not empty.
      if (smartRunsPoller.hasOutputDefinition(updatableRun)) {
        if (outputs != null) {
          smartRunsPoller.updateOutputAttributes(updatableRun, outputs);
        } else {
          Object cromwellOutputs = cromwellService.getOutputs(updatableRun.engineId());
          smartRunsPoller.updateOutputAttributes(updatableRun, cromwellOutputs);
        }
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
      return errorMessage;
    }
    return null;
  }

  private ArrayList<String> getRunErrors(Run updatableRun) {
    ArrayList<String> errors = new ArrayList<>();

    try {
      // Retrieve error from Cromwell
      String message = cromwellService.getRunErrors(updatableRun);
      if (!message.isEmpty()) {
        errors.add(message);
      }
    } catch (Exception e) {
      String errorMessage =
          "Error fetching Cromwell-level error from Cromwell for run %s"
              .formatted(updatableRun.runId());
      logger.error(errorMessage, e);
      errors.add(errorMessage);
    }
    return errors;
  }
}
