package bio.terra.cbas.runsets.results;

import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RunCompletionHandler {
  private final RunDao runDao;
  private final SmartRunsPoller smartRunsPoller;
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(RunCompletionHandler.class);

  public RunCompletionHandler(RunDao runDao, SmartRunsPoller smartRunsPoller) {
    this.runDao = runDao;
    this.smartRunsPoller = smartRunsPoller;
  }

  public RunResultsUpdateResult updateResults(
      Run updatableRun, CbasRunStatus status, Object outputs) {
    long updateResultsStartNanos = System.nanoTime();
    RunResultsUpdateResult updateResult = RunResultsUpdateResult.ERROR;

    try {
      var updatedRunState = status;

      if (updatableRun.status() == updatedRunState && outputs == null) {
        // Status is already up-to-date, there are no outputs to save.
        // Only update last polled timestamp.
        logger.info(
            "Update last modified timestamp for Run {} (engine ID {}) in status {}.",
            updatableRun.runId(),
            updatableRun.engineId(),
            updatableRun.status());

        var changes = runDao.updateLastPolledTimestamp(updatableRun.runId());
        if (changes != 1) {
          logger.warn(
              "Expected 1 row change updating last_polled_timestamp for Run {} in status {}, but got {}.",
              updatableRun.runId(),
              updatableRun.status(),
              changes);
          return RunResultsUpdateResult.ERROR;
        }
        return RunResultsUpdateResult.SUCCESS;
      }

      // Pull workflow completion information and save run outputs
      ArrayList<String> errors = new ArrayList<>();

      // Saving run outputs for a Successful status only.
      if (updatedRunState == CbasRunStatus.COMPLETE && outputs != null) {
        // Assuming that if no outputs were not passed with results,
        // Then no explicit pull should be made for outputs from Cromwell.
        String errorMessage = saveWorkflowOutputs(updatableRun, outputs);
        if (errorMessage != null && !errorMessage.isEmpty()) {
          // If this is the last attempt to save workflow outputs,
          // update run info in database with an error.
          // Otherwise, we should return internal error to caller.
          // Spike for handling final/fatal error is [WM-]
          errors.add(errorMessage);
          updatedRunState = CbasRunStatus.SYSTEM_ERROR;
        }
      } else if (updatedRunState.inErrorState()) {
        // Pull Cromwell errors for a run.
        // If request would have errors in the body, we can avoid this round trip.
        // [WM-2090] to address this issue.
        var cromwellErrors = smartRunsPoller.getWorkflowErrors(updatableRun);
        if (cromwellErrors != null && !cromwellErrors.isEmpty()) {
          errors.addAll(cromwellErrors);
        }
      }

      // Save the updated run record in database.
      updateResult = updateDatabaseRunStatus(updatableRun, updatedRunState, errors);
    } finally {
      recordMethodCompletion(
          updateResultsStartNanos, RunResultsUpdateResult.SUCCESS == updateResult);
    }
    // we should not come here
    return updateResult;
  }

  /*
   The method checks if output definitions are associated with run and makes updates.
   If outputs passed as arguments are null, but the workflow has the output definitions,
   then call to Cromwell will be made to pull the outputs.
  */
  private String saveWorkflowOutputs(Run updatableRun, Object outputs) {
    try {
      // we only write back output attributes to WDS if output definition is not empty.
      if (smartRunsPoller.hasOutputDefinition(updatableRun)) {
        smartRunsPoller.updateOutputAttributes(updatableRun, outputs);
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

  private RunResultsUpdateResult updateDatabaseRunStatus(
      Run updatableRun, CbasRunStatus updatedRunState, ArrayList<String> errors) {
    OffsetDateTime engineChangedTimestamp = DateUtils.currentTimeInUTC();
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
      return RunResultsUpdateResult.SUCCESS;
    }

    String databaseUpdateErrorMessage =
        "Run %s was attempted to update status from %s to %s but no DB rows were changed by the query."
            .formatted(updatableRun.runId(), updatableRun.status(), updatedRunState);
    logger.warn(databaseUpdateErrorMessage);
    return RunResultsUpdateResult.ERROR;
  }
}
