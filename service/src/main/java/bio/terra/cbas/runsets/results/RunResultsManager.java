package bio.terra.cbas.runsets.results;

import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.RunDao;
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
      RunDao runDao, SmartRunsPoller smartRunsPoller, CromwellService cromwellService) {
    this.runDao = runDao;
    this.smartRunsPoller = smartRunsPoller;
    this.cromwellService = cromwellService;
  }

  public RunResultsUpdateResponse updateResults(
      Run updatableRun, CbasRunStatus status, Object outputs) {
    long updateResultsStartNanos = System.nanoTime();
    boolean updateResultsSuccess = false;
    RunResultsUpdateResponse response = null;

    try {
      var updatedRunState = status;

      if (updatableRun.status() == updatedRunState && outputs == null) {
        // Status is already up-to-date, there are no outputs to save.
        // Only update last polled timestamp.
        logger.info(
            "Update last polled timestamp for Run {} (engine ID {}) in status {}.",
            updatableRun.runId(),
            updatableRun.engineId(),
            updatableRun.status());
        var changes = runDao.updateLastPolledTimestamp(updatableRun.runId());
        updateResultsSuccess = true;
        if (changes != 1) {
          logger.warn(
              "Expected 1 row change updating last_polled_timestamp for Run {} in status {}, but got {}.",
              updatableRun.runId(),
              updatableRun.status(),
              changes);
        }
        response = new RunResultsUpdateResponse(updateResultsSuccess, updatableRun.errorMessages());
      } else {
        // Pull workflow completion information and save run outputs
        ArrayList<String> errors = new ArrayList<>();

        // Saving run outputs for a Successful status only.
        if (updatedRunState == CbasRunStatus.COMPLETE && outputs != null) {
          // Assuming that if no outputs were not passed with results,
          // Then no explicit pull should be made for outputs from Cromwell.
          String errorMessage = saveRunOutputs(updatableRun, outputs);
          if (errorMessage != null && !errorMessage.isEmpty()) {
            errors.add(errorMessage);
            updatedRunState = CbasRunStatus.SYSTEM_ERROR;
          }
        } else if (updatedRunState.inErrorState()) {
          // Pull Cromwell errors for a run
          var cromwellErrors = getRunErrors(updatableRun);
          if (!cromwellErrors.isEmpty()) {
            errors.addAll(cromwellErrors);
          }
        }

        // Save the updated run record in database.
        response = updateDatabaseRunStatus(updatableRun, updatedRunState, errors);
      }
    } finally {
      recordMethodCompletion(updateResultsStartNanos, updateResultsSuccess);
    }
    return response;
  }

  /*
   The method checks if output definitions are associated with run and makes updates.
   If outputs passed as arguments are null, but the workflow has the output definitions,
   then call to Cromwell will be made to pull the outputs.
  */
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
    // This is a copy from SmartRunsPoller until we remove
    // workflow completion handling from a SmartRunsPoller.
    // Pending task [WM-2090].
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

  private RunResultsUpdateResponse updateDatabaseRunStatus(
      Run updatableRun, CbasRunStatus updatedRunState, ArrayList<String> errors) {
    OffsetDateTime engineChangedTimestamp = DateUtils.currentTimeInUTC();
    boolean updateResultsSuccess = false;
    int changes;

    if (errors.isEmpty()) {
      logger.info(
          "Updating status of Run {} (engine ID {}) from {} to {} with no errors",
          updatableRun.runId(),
          updatableRun.engineId(),
          updatableRun.status(),
          updatedRunState);
      changes =
          runDao.updateRunStatus(updatableRun.runId(), updatedRunState, engineChangedTimestamp);
    } else {
      logger.info(
          "Updating status of Run {} (engine ID {}) from {} to {} with {} errors",
          updatableRun.runId(),
          updatableRun.engineId(),
          updatableRun.status(),
          updatedRunState,
          errors.size());
      updatableRun = updatableRun.withErrorMessages(String.join(", ", errors));
      changes =
          runDao.updateRunStatusWithError(
              updatableRun.runId(),
              updatedRunState,
              engineChangedTimestamp,
              updatableRun.errorMessages());
    }
    if (changes == 1) {
      updateResultsSuccess = true;
      updatableRun =
          updatableRun
              .withStatus(updatedRunState)
              .withLastModified(engineChangedTimestamp)
              .withLastPolled(OffsetDateTime.now());
    } else {
      String databaseUpdateErrorMessage =
          "Run %s was attempted to update status from %s to %s but no DB rows were changed by the query."
              .formatted(updatableRun.runId(), updatableRun.status(), updatedRunState);
      logger.warn(databaseUpdateErrorMessage);
      // Overriding all previous errors as they are not relevant for the reported result.
      updatableRun = updatableRun.withErrorMessages(databaseUpdateErrorMessage);
    }
    return new RunResultsUpdateResponse(updateResultsSuccess, updatableRun.errorMessages());
  }
}
