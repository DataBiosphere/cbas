package bio.terra.cbas.runsets.monitoring;

import static java.util.stream.Collectors.groupingBy;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.models.Run;
import cromwell.client.ApiException;
import cromwell.client.model.State;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
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

  public List<Run> updateRuns(List<Run> runs) {

    // For metrics:
    OffsetDateTime startTime = OffsetDateTime.now();

    // Filter only updatable runs:
    Stream<Run> updatableRuns = runs.stream().filter(this::nonTerminal);

    // This has the nice outcome of counting even if the size is 0, which means this metric
    // is created and stays current even if nothing is being updated:
    logger.debug("METRIC: COUNT runs needing status update: {}", updatableRuns.count());

    // Group by current (engine) status:
    Map<State, List<Run>> engineStatus =
        updatableRuns.collect(
            groupingBy(
                r -> {
                  try {
                    var result = cromwellService.runStatus(r.engineId()).getState();
                    logger.debug("METRIC: INCREMENT runs polled for status update successfully");
                    return result;
                  } catch (ApiException e) {
                    logger.warn("Unable to fetch updated status for run {}.", r.id(), e);
                    logger.debug("METRIC: INCREMENT runs polled for status update unsuccessfully");
                    return State.fromValue(r.status());
                  }
                }));

    Set<Run> updatedRuns = new HashSet<>(runs);

    for (Map.Entry<State, List<Run>> stateEntry : engineStatus.entrySet()) {
      for (Run r : stateEntry.getValue()) {
        if (!r.status().equals(stateEntry.getKey().toString())) {
          logger.debug("Updating status of Run {} (engine ID {})", r.id(), r.engineId());
          logger.debug("METRIC: INCREMENT runs transitioned to final status");
          var changes = runDao.updateRunStatus(r, stateEntry.getKey().toString());
          if (changes == 1) {
            updatedRuns.remove(r);
            updatedRuns.add(r.withStatus(stateEntry.getKey().toString()));
          }
        }
      }
    }

    var result = List.copyOf(updatedRuns);
    var pollAndUpdateMillis = ChronoUnit.MILLIS.between(startTime, OffsetDateTime.now());
    logger.debug("METRIC: TIMER smart status poll: {}", pollAndUpdateMillis);
    return result;
  }

  private boolean isTerminal(Run run) {
    RunState state = RunState.fromValue(run.status());
    return RunState.CANCELED.equals(state)
        || RunState.COMPLETE.equals(state)
        || RunState.EXECUTOR_ERROR.equals(state)
        || RunState.SYSTEM_ERROR.equals(state);
  }

  private boolean nonTerminal(Run run) {
    return !isTerminal(run);
  }
}
