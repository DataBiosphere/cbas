package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.common.MetricsUtil.increaseEventCounter;
import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;
import static bio.terra.cbas.common.MetricsUtil.recordOutboundApiRequestCompletion;

import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.outputs.OutputGenerator;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.ApiException;
import cromwell.client.model.WorkflowQueryResult;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

  public static record UpdateRunsResult(
      List<Run> updatedRuns, long polledCount, boolean fullyUpdated) {}

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
          OutputProcessingException {
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
  public UpdateRunsResult updateRuns(List<Run> runs) {
    // For metrics:
    long startTimeNs = System.nanoTime();
    boolean successBoolean = false;

    try {
      // Filter only updatable runs:
      PickedUpdatableRuns updatableRuns =
          pickUpdatableRuns(runs, cbasApiConfiguration.getMaxSmartPollRunUpdates());

      increaseEventCounter("run updates required", updatableRuns.totalUpdatable);

      List<Run> updatedRuns =
          runs.stream()
              .map(r -> updatableRuns.selectedRuns.contains(r) ? tryUpdateRun(r) : r)
              .collect(Collectors.toList());

      increaseEventCounter("run updates polled", updatableRuns.selectedRuns.size());
      successBoolean = true;
      return new UpdateRunsResult(
          updatedRuns,
          updatableRuns.selectedRuns.size(),
          updatableRuns.selectedRuns.size() == updatableRuns.totalUpdatable);
    } finally {
      recordMethodCompletion(startTimeNs, successBoolean);
    }
  }

  protected record PickedUpdatableRuns(Set<Run> selectedRuns, int totalUpdatable) {}

  protected static PickedUpdatableRuns pickUpdatableRuns(List<Run> runs, int limit) {

    List<Run> updatableRuns = runs.stream().filter(r -> r.status().nonTerminal()).toList();

    // Now sort and limit by last polled timestamp, to select "least recently polled" workflows
    // first:
    return new PickedUpdatableRuns(
        updatableRuns.stream()
            .sorted(Comparator.comparing(Run::lastPolledTimestamp))
            .limit(limit)
            .collect(Collectors.toSet()),
        updatableRuns.size());
  }

  private Run tryUpdateRun(Run r) {
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
        if (updatedRunState == CbasRunStatus.COMPLETE) {
          try {
            // we only write back output attributes to WDS if output definition is not empty. This
            // is to avoid sending empty PATCH requests to WDS
            if (hasOutputDefinition(updatableRun)) {
              updateOutputAttributes(updatableRun);
            }
          } catch (Exception e) {
            // log error and mark Run as Failed
            // TODO: When epic WM-1433 is being worked on, add error message in database stating
            //  updating output attributes failed for this particular Run.
            logger.error(
                "Error while updating attributes for record {} from run {}.",
                updatableRun.recordId(),
                updatableRun.runId(),
                e);
            updatedRunState = CbasRunStatus.SYSTEM_ERROR;
          }
        } else if (updatedRunState.inErrorState()) {
          try {
            // Retrieve error from Cromwell
            String message = cromwellService.getRunErrors(updatableRun);
            if (!message.isEmpty()) {
              var updatedRun = runDao.updateErrorMessage(updatableRun.runId(), message);
              if (updatedRun == 1) {
                updatableRun = updatableRun.withErrorMessage(message);
              }
            }
          } catch (Exception e) {
            logger.error(
                "Error fetching Cromwell-level error from Cromwell for run {}.",
                updatableRun.runId(),
                e);
          }
        }
        logger.info(
            "Updating status of Run {} (engine ID {}) from {} to {}",
            updatableRun.runId(),
            updatableRun.engineId(),
            updatableRun.status(),
            updatedRunState);
        var changes =
            runDao.updateRunStatus(updatableRun.runId(), updatedRunState, engineStatusChanged);
        if (changes == 1) {
          updatableRun =
              updatableRun.withStatus(updatedRunState).withLastModified(engineStatusChanged);
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
