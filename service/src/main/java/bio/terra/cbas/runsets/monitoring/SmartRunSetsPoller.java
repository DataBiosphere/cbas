package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.common.MetricsUtil.increaseEventCounter;
import static bio.terra.cbas.common.MetricsUtil.recordMethodCompletion;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class SmartRunSetsPoller {

  private final SmartRunsPoller smartRunsPoller;
  private final RunDao runDao;
  private final RunSetDao runSetDao;

  public SmartRunSetsPoller(SmartRunsPoller smartRunsPoller, RunSetDao runSetDao, RunDao runDao) {
    this.runDao = runDao;
    this.runSetDao = runSetDao;
    this.smartRunsPoller = smartRunsPoller;
  }

  public List<RunSet> updateRunSets(List<RunSet> runSets) {
    // For metrics:
    long startTimeNs = System.nanoTime();
    boolean successBoolean = false;

    try {
      var dividedByUpdateNeeded =
          runSets.stream().collect(Collectors.partitioningBy(r -> r.status().nonTerminal()));

      increaseEventCounter("run set status updated", dividedByUpdateNeeded.get(true).size());

      List<RunSet> result =
          Stream.concat(
                  dividedByUpdateNeeded.get(false).stream(),
                  dividedByUpdateNeeded.get(true).stream().map(this::updateRunSet))
              .toList();
      successBoolean = true;
      return result;
    } finally {
      recordMethodCompletion(startTimeNs, successBoolean);
    }
  }

  private RunSet updateRunSet(RunSet rs) {

    // For metrics:
    long startTimeNs = System.nanoTime();
    boolean successBoolean = false;
    try {
      List<Run> updateableRuns =
          runDao.getRuns(new RunDao.RunsFilters(rs.runSetId(), CbasRunStatus.NON_TERMINAL_STATES));

      smartRunsPoller.updateRuns(updateableRuns);

      StatusAndCounts newStatusAndCounts = newStatusAndErrorCounts(rs);
      if (newStatusAndCounts.status != rs.status()
          || !Objects.equals(newStatusAndCounts.runErrors, rs.errorCount())
          || !Objects.equals(newStatusAndCounts.totalRuns, rs.runCount())) {
        // Update and re-fetch:
        runSetDao.updateStateAndRunDetails(
            rs.runSetId(),
            newStatusAndCounts.status(),
            newStatusAndCounts.totalRuns(),
            newStatusAndCounts.runErrors());
      } else {
        runSetDao.updateLastPolled(List.of(rs.runSetId()));
      }
      successBoolean = true;
      return runSetDao.getRunSet(rs.runSetId());
    } finally {
      recordMethodCompletion(startTimeNs, successBoolean);
    }
  }

  private record StatusAndCounts(CbasRunSetStatus status, Integer totalRuns, Integer runErrors) {}

  private StatusAndCounts newStatusAndErrorCounts(RunSet rs) {
    Map<CbasRunStatus, Integer> runStatusCounts =
        runDao.getRunStatusCounts(new RunDao.RunsFilters(rs.runSetId(), null));
    return new StatusAndCounts(
        CbasRunSetStatus.fromRunStatuses(runStatusCounts),
        runStatusCounts.values().stream().mapToInt(Integer::intValue).sum(),
        runStatusCounts.getOrDefault(CbasRunStatus.SYSTEM_ERROR, 0)
            + runStatusCounts.getOrDefault(CbasRunStatus.EXECUTOR_ERROR, 0));
  }
}
