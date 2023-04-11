package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.common.MetricsUtil.increaseEventCounter;
import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;
import static bio.terra.cbas.common.MetricsUtil.recordOutboundApiRequestCompletion;

import bio.terra.cbas.common.MetricsUtil;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.cbas.runsets.outputs.OutputGenerator;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.ApiException;
import cromwell.client.model.WorkflowQueryResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmartRunsPoller {

  private final CromwellService cromwellService;
  private final RunDao runDao;

  private final WdsService wdsService;
  private final ObjectMapper objectMapper;

  private final CbasApiConfiguration cbasApiConfiguration;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SmartRunsPoller.class);

  public SmartRunsPoller(
      CromwellService cromwellService,
      RunDao runDao,
      WdsService wdsService,
      ObjectMapper objectMapper,
      CbasApiConfiguration cbasApiConfiguration) {
    this.cromwellService = cromwellService;
    this.runDao = runDao;
    this.wdsService = wdsService;
    this.objectMapper = objectMapper;
    this.cbasApiConfiguration = cbasApiConfiguration;
  }

  public boolean hasOutputDefinition(Run run) throws JsonProcessingException {
    List<WorkflowOutputDefinition> outputDefinitionList =
        objectMapper.readValue(run.runSet().outputDefinition(), new TypeReference<>() {});
    return !outputDefinitionList.isEmpty();
  }

  public void updateOutputAttributes(Run run)
      throws ApiException, JsonProcessingException,
          org.databiosphere.workspacedata.client.ApiException, CoercionException,
          OutputProcessingException, DependencyNotAvailableException {
    List<WorkflowOutputDefinition> outputDefinitionList =
        objectMapper.readValue(run.runSet().outputDefinition(), new TypeReference<>() {});
    Object outputs = cromwellService.getOutputs(run.engineId());
    RecordAttributes outputParamDef = OutputGenerator.buildOutputs(outputDefinitionList, outputs);
    RecordRequest request = new RecordRequest().attributes(outputParamDef);

    logger.info(
        "Updating output attributes for Record ID {} from Run {}.", run.recordId(), run.engineId());

    wdsService.updateRecord(request, run.runSet().recordType(), run.recordId());
  }

  /**
   * Updates a list of runs by checking with the engine whether any non-terminal statuses have
   * changed and if so, updating the database.
   *
   * @param runs The list of input runs to check for updates
   * @return A new list containing up-to-date run information for all runs in the input
   */
  public UpdateResult<Run> updateRuns(List<Run> runs) {
    return updateRuns(runs, Optional.empty());
  }

  /**
   * Updates a list of runs by checking with the engine whether any non-terminal statuses have
   * changed and if so, updating the database.
   *
   * @param runs The list of input runs to check for updates
   * @return A new list containing up-to-date run information for all runs in the input
   */
  public UpdateResult<Run> updateRuns(List<Run> runs, Optional<OffsetDateTime> customEndTime) {
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
              this::tryUpdateRun,
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

  private Run tryUpdateRun(Run r) {
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

    CbasRunStatus newStatus = CbasRunStatus.INITIALIZING;
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
      return updateDatabaseRunStatus(newStatus, engineChangedTimestamp, r);
    } catch (Exception e) {
      logger.warn("Unable to update run details for {} in database.", r.runId(), e);
      return r;
    }
  }

  private Run updateDatabaseRunStatus(
      CbasRunStatus status, OffsetDateTime engineStatusChanged, Run updatableRun) {
    long updateDatabaseRunStatusStartNanos = System.nanoTime();
    boolean updateDatabaseRunStatusSuccess = false;

    try {
      var updatedRunState = status;
      if (updatableRun.status() != updatedRunState) {
        ArrayList<String> errors = new ArrayList<>();

        if (updatedRunState == CbasRunStatus.COMPLETE) {
          try {
            // we only write back output attributes to WDS if output definition is not empty. This
            // is to avoid sending empty PATCH requests to WDS
            if (hasOutputDefinition(updatableRun)) {
              updateOutputAttributes(updatableRun);
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
            errors.add(errorMessage);
            updatedRunState = CbasRunStatus.SYSTEM_ERROR;
          }
        } else if (updatedRunState.inErrorState()) {
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
        }
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
              runDao.updateRunStatus(updatableRun.runId(), updatedRunState, engineStatusChanged);
        } else {
          updatableRun = updatableRun.withErrorMessages(String.join(", ", errors));
          changes =
              runDao.updateRunStatusWithError(
                  updatableRun.runId(),
                  updatedRunState,
                  engineStatusChanged,
                  updatableRun.errorMessages());
        }
        if (changes == 1) {
          updatableRun =
              updatableRun
                  .withStatus(updatedRunState)
                  .withLastModified(engineStatusChanged)
                  .withLastPolled(OffsetDateTime.now());
        } else {
          logger.warn(
              "Run {} was identified for updating status from {} to {} but no DB rows were changed by the query.",
              updatableRun.runId(),
              updatableRun.status(),
              updatedRunState);
        }
      } else {
        // if run status hasn't changed, only update last polled timestamp
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
      recordMethodCompletion(updateDatabaseRunStatusStartNanos, updateDatabaseRunStatusSuccess);
    }
    return updatableRun;
  }
}
