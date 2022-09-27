package bio.terra.cbas.runsets.monitoring;

import static java.util.stream.Collectors.groupingBy;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.Run;
import cromwell.client.ApiException;
import cromwell.client.model.State;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmartRunsPoller {

  private final CromwellService cromwellService;
  private final RunDao runDao;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SmartRunsPoller.class);

  public SmartRunsPoller(CromwellService cromwellService, RunDao runDao) {
    this.cromwellService = cromwellService;
    this.runDao = runDao;
  }

  /**
   * Updates a list of runs by:
   *  - Checking with the engine whether any non-terminal statuses have changed
   *  - If so, updating the database
   * @param runs The list of input runs to check for updates
   * @return A new list containing up-to-date run information for all runs in the input
   */
  public List<Run> updateRuns(List<Run> runs) {

    // For metrics:
    OffsetDateTime startTime = OffsetDateTime.now();

    // Filter only updatable runs:
    List<Run> updatableRuns = runs.stream().filter(Run::nonTerminal).toList();

    // This has the nice outcome of counting even if the size is 0, which means this metric
    // is created and stays current even if nothing is being updated:
    logger.debug("METRIC: COUNT runs needing status update: {}", updatableRuns.size());

    // Group by current (engine) status:
    Map<State, List<Run>> engineStatuses =
        updatableRuns.stream()
            .collect(
                groupingBy(
                    r -> {
                      try {
                        var result = cromwellService.runStatus(r.engineId()).getState();
                        logger.debug(
                            "METRIC: INCREMENT runs polled for status update successfully");
                        return result;
                      } catch (ApiException e) {
                        logger.warn("Unable to fetch updated status for run {}.", r.id(), e);
                        logger.debug(
                            "METRIC: INCREMENT runs polled for status update unsuccessfully");
                        return State.fromValue(r.status());
                      }
                    }));

    Set<Run> updatedRuns = new HashSet<>(runs);

    for (Map.Entry<State, List<Run>> engineStateEntry : engineStatuses.entrySet()) {
      for (Run r : engineStateEntry.getValue()) {
        var currentState = engineStateEntry.getKey().toString();
        if (!r.status().equals(currentState)) {
          logger.debug("Updating status of Run {} (engine ID {})", r.id(), r.engineId());
          var changes = runDao.updateRunStatus(r, currentState);
          if (changes == 1) {
            logger.debug("METRIC: INCREMENT runs transitioned to final status successfully");
            updatedRuns.remove(r);
            updatedRuns.add(r.withStatus(currentState));
          } else {
            logger.debug("METRIC: INCREMENT runs transitioned to final status unsuccessfully");
            logger.warn("Run {} was identified for updating status from {} to {} but no DB rows were changed by the query.", r.id(), r.status(), currentState);
          }
        }
      }
    }

    var result = List.copyOf(updatedRuns);
    var pollAndUpdateMillis = ChronoUnit.MILLIS.between(startTime, OffsetDateTime.now());
    logger.debug("METRIC: TIMER smart status poll: {}", pollAndUpdateMillis);
    return result;
  }
}
