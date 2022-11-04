package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.common.MetricsUtil.increaseEventCounter;
import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;
import static bio.terra.cbas.common.MetricsUtil.recordOutboundApiRequestCompletion;
import static java.util.stream.Collectors.groupingBy;

import bio.terra.cbas.common.exceptions.WorkflowOutputNotFoundException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SmartRunsPoller.class);

  public SmartRunsPoller(
      CromwellService cromwellService,
      RunDao runDao,
      WdsService wdsService,
      ObjectMapper objectMapper) {
    this.cromwellService = cromwellService;
    this.runDao = runDao;
    this.wdsService = wdsService;
    this.objectMapper = objectMapper;
  }

  public boolean hasOutputDefinition(Run run) throws JsonProcessingException {
    List<WorkflowOutputDefinition> outputDefinitionList =
        objectMapper.readValue(run.runSet().method().outputDefinition(), new TypeReference<>() {});

    return !outputDefinitionList.isEmpty();
  }

  public void updateOutputAttributes(Run run)
      throws WorkflowOutputNotFoundException, ApiException, JsonProcessingException,
      org.databiosphere.workspacedata.client.ApiException, CoercionException {
    List<WorkflowOutputDefinition> outputDefinitionList =
        objectMapper.readValue(run.runSet().method().outputDefinition(), new TypeReference<>() {});

    Object outputs = cromwellService.getOutputs(run.engineId());
    RecordAttributes outputParamDef = OutputGenerator.buildOutputs(outputDefinitionList, outputs);
    RecordRequest request = new RecordRequest().attributes(outputParamDef);

    logger.info(
        "Updating output attributes for Record ID {} from Run {}.", run.recordId(), run.engineId());

    wdsService.updateRecord(request, run.runSet().method().recordType(), run.recordId());
  }

  /**
   * Updates a list of runs by checking with the engine whether any non-terminal statuses have
   * changed and if so, updating the database.
   *
   * @param runs The list of input runs to check for updates
   * @return A new list containing up-to-date run information for all runs in the input
   */
  public List<Run> updateRuns(List<Run> runs) {

    // For metrics:
    long startTimeNs = System.nanoTime();
    boolean successBoolean = false;

    try {
      // Filter only updatable runs:
      List<Run> updatableRuns = runs.stream().filter(r -> r.status().nonTerminal()).toList();

      increaseEventCounter("status update required", updatableRuns.size());

      // Group by current (engine) status:
      Map<CbasRunStatus, List<Run>> engineStatuses =
          updatableRuns.stream()
              .collect(
                  groupingBy(
                      r -> {
                        long getStatusStartNanos = System.nanoTime();
                        boolean getStatusSuccess = false;
                        try {
                          var result =
                              CbasRunStatus.fromValue(
                                  cromwellService.runStatus(r.engineId()).getState());
                          getStatusSuccess = true;
                          return result;
                        } catch (ApiException | IllegalArgumentException e) {
                          logger.warn("Unable to fetch updated status for run {}.", r.id(), e);
                          return r.status();
                        } finally {
                          recordOutboundApiRequestCompletion(
                              "wes/runStatus", getStatusStartNanos, getStatusSuccess);
                        }
                      }));

      Set<Run> updatedRuns = new HashSet<>(runs);

      for (Map.Entry<CbasRunStatus, List<Run>> engineStateEntry : engineStatuses.entrySet()) {
        for (Run r : engineStateEntry.getValue()) {
          updateDatabaseRunStatus(updatedRuns, engineStateEntry, r);
        }
      }
      successBoolean = true;
      return List.copyOf(updatedRuns);
    } finally {
      recordMethodCompletion(startTimeNs, successBoolean);
    }
  }

  private void updateDatabaseRunStatus(
      Set<Run> updatedRuns, Map.Entry<CbasRunStatus, List<Run>> engineStateEntry, Run r) {
    long updateDatabaseRunStatusStartNanos = System.nanoTime();
    boolean updateDatabaseRunStatusSuccess = false;

    try {
      var updatedRunState = engineStateEntry.getKey();
      if (r.status() != updatedRunState) {
        if (updatedRunState == CbasRunStatus.COMPLETE) {
          try {
            // we only write back output attributes to WDS if output definition is not empty. This
            // is to avoid sending empty PATCH requests to WDS
            if (hasOutputDefinition(r)) {
              updateOutputAttributes(r);
            }
          } catch (Exception e) {
            // log error and mark Run as Failed
            // TODO: When epic WM-1433 is being worked on, add error message in database stating
            //  updating output attributes failed for this particular Run.
            logger.error(
                "Error while updating attributes for record {} from run {}.",
                r.recordId(),
                r.id(),
                e);
            updatedRunState = CbasRunStatus.SYSTEM_ERROR;
          }
        }
        logger.debug(
            "Updating status of Run {} (engine ID {}) from {} to {}",
            r.id(),
            r.engineId(),
            r.status(),
            updatedRunState);
        var changes = runDao.updateRunStatus(r, updatedRunState);
        if (changes == 1) {
          updatedRuns.remove(r);
          updatedRuns.add(r.withStatus(updatedRunState));
        } else {
          logger.warn(
              "Run {} was identified for updating status from {} to {} but no DB rows were changed by the query.",
              r.id(),
              r.status(),
              updatedRunState);
        }
      } else {
        // if run status hasn't changed, only update last polled timestamp
        var changes = runDao.updateLastPolledTimestamp(r.id());
        if (changes != 1) {
          logger.warn(
              "Expected 1 row change updating last_polled_timestamp for Run {} in status {}, but got {}.",
              r.id(),
              r.status(),
              changes);
        }
      }
    } finally {
      recordMethodCompletion(updateDatabaseRunStatusStartNanos, updateDatabaseRunStatusSuccess);
    }
  }
}
