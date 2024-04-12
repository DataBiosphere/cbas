package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.common.MetricsUtil.increaseEventCounter;
import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;
import static bio.terra.cbas.common.MetricsUtil.recordOutboundApiRequestCompletion;

import bio.terra.cbas.common.MetricsUtil;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dependencies.wds.WdsClientUtils;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.cbas.runsets.results.RunCompletionHandler;
import bio.terra.cbas.runsets.results.RunCompletionResult;
import bio.terra.common.iam.BearerToken;
import cromwell.client.ApiException;
import cromwell.client.model.WorkflowQueryResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmartRunsPoller {

  private final CromwellService cromwellService;
  private final RunCompletionHandler runCompletionHandler;
  private final CbasApiConfiguration cbasApiConfiguration;
  private final MicrometerMetrics micrometerMetrics;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SmartRunsPoller.class);

  public SmartRunsPoller(
      CromwellService cromwellService,
      RunCompletionHandler runCompletionHandler,
      CbasApiConfiguration cbasApiConfiguration,
      MicrometerMetrics micrometerMetrics) {
    this.cromwellService = cromwellService;
    this.runCompletionHandler = runCompletionHandler;
    this.cbasApiConfiguration = cbasApiConfiguration;
    this.micrometerMetrics = micrometerMetrics;
  }

  /**
   * Updates a list of runs by checking with the engine whether any non-terminal statuses have
   * changed and if so, updating the database.
   *
   * @param runs The list of input runs to check for updates
   * @return A new list containing up-to-date run information for all runs in the input
   */
  public UpdateResult<Run> updateRuns(List<Run> runs, BearerToken userToken) {
    return updateRuns(runs, Optional.empty(), userToken);
  }

  /**
   * Updates a list of runs by checking with the engine whether any non-terminal statuses have
   * changed and if so, updating the database.
   *
   * @param runs The list of input runs to check for updates
   * @return A new list containing up-to-date run information for all runs in the input
   */
  public UpdateResult<Run> updateRuns(
      List<Run> runs, Optional<OffsetDateTime> customEndTime, BearerToken userToken) {
    // For metrics:
    long startTimeNs = System.nanoTime();
    boolean successBoolean = false;

    OffsetDateTime actualEndTime =
        customEndTime.orElse(
            OffsetDateTime.now()
                .plusSeconds(cbasApiConfiguration.getMaxSmartPollRunUpdateSeconds()));

    try {
      UpdateResult<Run> runUpdateResult =
          TimeLimitedUpdater.update(
              runs,
              Run::runId,
              r ->
                  r.status().nonTerminal()
                      && r.lastPolledTimestamp()
                          .isBefore(
                              OffsetDateTime.now()
                                  .minus(
                                      Duration.ofSeconds(
                                          cbasApiConfiguration
                                              .getMinSecondsBetweenRunStatusPolls()))),
              Comparator.comparing(Run::lastPolledTimestamp),
              r -> tryUpdateRun(r, userToken),
              actualEndTime);

      increaseEventCounter("run updates required", runUpdateResult.totalEligible());
      increaseEventCounter("run updates polled", runUpdateResult.totalUpdated());

      successBoolean = true;
      logger.info(
          "Status update operation completed in %f ms (polling %d of %d possible runs)"
              .formatted(
                  MetricsUtil.sinceInMilliseconds(startTimeNs),
                  runUpdateResult.totalUpdated(),
                  runUpdateResult.totalEligible()));

      return runUpdateResult;
    } finally {
      recordMethodCompletion(startTimeNs, successBoolean);
    }
  }

  private Run tryUpdateRun(Run r, BearerToken userToken) {
    logger.info("Fetching update for run %s".formatted(r.runId()));
    // Get the new workflow summary:
    long getStatusStartNanos = System.nanoTime();
    boolean getStatusSuccess = false;
    WorkflowQueryResult newWorkflowSummary;
    try {
      newWorkflowSummary = cromwellService.runSummary(r.engineId());
    } catch (ApiException | IllegalArgumentException e) {
      logger.warn("Unable to fetch summary for run {}.", r.runId(), e);
      return r;
    } finally {
      recordOutboundApiRequestCompletion("wes/runSummary", getStatusStartNanos, getStatusSuccess);
    }

    CbasRunStatus newStatus = CbasRunStatus.UNKNOWN;
    if (newWorkflowSummary != null) {
      newStatus = CbasRunStatus.fromCromwellStatus(newWorkflowSummary.getStatus());
    }

    OffsetDateTime engineChangedTimestamp = null;
    if (newWorkflowSummary != null) {
      engineChangedTimestamp =
          Optional.ofNullable(newWorkflowSummary.getEnd())
              .orElse(
                  Optional.ofNullable(newWorkflowSummary.getStart())
                      .orElse(newWorkflowSummary.getSubmission()));
    }

    try {
      return updateDatabaseRunStatus(newStatus, engineChangedTimestamp, r, userToken);
    } catch (Exception e) {
      logger.warn("Unable to update run details for {} in database.", r.runId(), e);
      return r;
    }
  }

  private List<String> getWorkflowErrors(Run updatableRun) {
    ArrayList<String> errors = new ArrayList<>();
    try {
      // Retrieve error from Cromwell
      String message = cromwellService.getRunErrors(updatableRun);
      if (!message.isEmpty()) {
        errors.add(message);
      }
    } catch (Exception e) {
      String errorMessage = "Error fetching Cromwell-level error.";
      logger.error(errorMessage, e);
      errors.add("%s Details: %s".formatted(errorMessage, e.getMessage()));
    }
    return errors;
  }

  private Run updateDatabaseRunStatus(
      CbasRunStatus status,
      OffsetDateTime engineStatusChanged,
      Run updatableRun,
      BearerToken userToken) {
    long updateDatabaseRunStatusStartNanos = System.nanoTime();
    boolean updateDatabaseRunStatusSuccess = false;
    ArrayList<String> errors = new ArrayList<>();
    Object outputs = null;

    try {
      var updatedRunState = status;
      if (updatedRunState == CbasRunStatus.COMPLETE) {
        // Retrieve workflow outputs
        try {
          outputs = cromwellService.getOutputs(updatableRun.engineId());
        } catch (Exception e) {
          // log error and mark Run as Failed
          String errorMessage =
              "Error while retrieving workflow outputs for record %s from run %s (engine workflow ID %s): %s"
                  .formatted(
                      updatableRun.recordId(),
                      updatableRun.runId(),
                      updatableRun.engineId(),
                      WdsClientUtils.extractErrorMessage(e.getMessage()));
          logger.error(errorMessage, e);
          errors.add(errorMessage);
          updatedRunState = CbasRunStatus.SYSTEM_ERROR;
        }
      } else if (updatedRunState.inErrorState()) {
        // Retrieve workflow errors
        var cromwellErrors = getWorkflowErrors(updatableRun);
        if (!cromwellErrors.isEmpty()) {
          errors.addAll(cromwellErrors);
        }
      }
      // Call Run Completion handler to update results
      micrometerMetrics.logRunCompletion("smartpoller", updatedRunState);
      var updateResult =
          runCompletionHandler.updateResults(
              updatableRun, updatedRunState, outputs, errors, engineStatusChanged, userToken);
      updateDatabaseRunStatusSuccess = (updateResult == RunCompletionResult.SUCCESS);
    } finally {
      recordMethodCompletion(updateDatabaseRunStatusStartNanos, updateDatabaseRunStatusSuccess);
    }
    return updatableRun;
  }
}
