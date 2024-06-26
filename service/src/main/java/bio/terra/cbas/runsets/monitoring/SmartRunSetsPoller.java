package bio.terra.cbas.runsets.monitoring;

import bio.terra.cbas.common.MetricsUtil;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.common.iam.BearerToken;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmartRunSetsPoller {

  private final SmartRunsPoller smartRunsPoller;
  private final RunDao runDao;
  private final RunSetDao runSetDao;
  private final CbasApiConfiguration cbasApiConfiguration;
  private final MicrometerMetrics micrometerMetrics;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SmartRunSetsPoller.class);

  public SmartRunSetsPoller(
      SmartRunsPoller smartRunsPoller,
      RunSetDao runSetDao,
      RunDao runDao,
      CbasApiConfiguration cbasApiConfiguration,
      MicrometerMetrics micrometerMetrics) {
    this.runDao = runDao;
    this.runSetDao = runSetDao;
    this.smartRunsPoller = smartRunsPoller;
    this.cbasApiConfiguration = cbasApiConfiguration;
    this.micrometerMetrics = micrometerMetrics;
  }

  public UpdateResult<RunSet> updateRunSets(List<RunSet> runSets, BearerToken userToken) {
    long startTimeNs = System.nanoTime();

    // For metrics:
    Timer.Sample methodStartSample = micrometerMetrics.startTimer();
    boolean successBoolean = false;

    OffsetDateTime limitedEndTime =
        OffsetDateTime.now().plusSeconds(cbasApiConfiguration.getMaxSmartPollRunSetUpdateSeconds());

    try {
      TimeLimitedUpdater.UpdateResult<RunSet> runSetUpdateResult =
          TimeLimitedUpdater.update(
              runSets,
              RunSet::runSetId,
              rs -> rs.status().nonTerminal(),
              Comparator.comparing(RunSet::lastPolledTimestamp),
              rs -> updateRunSet(rs, limitedEndTime, userToken),
              limitedEndTime);

      micrometerMetrics.increaseEventCounter(
          "run_set_updates_required", runSetUpdateResult.totalEligible());
      micrometerMetrics.increaseEventCounter(
          "run_set_updates_polled", runSetUpdateResult.totalUpdated());

      successBoolean = true;
      logger.info(
          "Run set status update operation completed in %f ms (polling %d of %d possible run sets)"
              .formatted(
                  MetricsUtil.sinceInMilliseconds(startTimeNs),
                  runSetUpdateResult.totalUpdated(),
                  runSetUpdateResult.totalEligible()));

      return runSetUpdateResult;
    } finally {
      micrometerMetrics.recordMethodCompletion(methodStartSample, successBoolean);
    }
  }

  private RunSet updateRunSet(RunSet rs, OffsetDateTime limitedEndTime, BearerToken userToken) {
    // For metrics:
    Timer.Sample methodStartSample = micrometerMetrics.startTimer();
    boolean successBoolean = false;

    try {
      List<Run> updateableRuns =
          runDao.getRuns(new RunDao.RunsFilters(rs.runSetId(), CbasRunStatus.NON_TERMINAL_STATES));

      // Make sure neither the run set nor run time limit expire:
      OffsetDateTime runPollerSpecificUpdateLimit =
          OffsetDateTime.now().plusSeconds(cbasApiConfiguration.getMaxSmartPollRunUpdateSeconds());
      OffsetDateTime runPollUpdateEndTime =
          runPollerSpecificUpdateLimit.isBefore(limitedEndTime)
              ? runPollerSpecificUpdateLimit
              : limitedEndTime;

      smartRunsPoller.updateRuns(updateableRuns, Optional.of(runPollUpdateEndTime), userToken);

      StatusAndCounts newStatusAndCounts = newStatusAndErrorCounts(rs);

      if (rs.status() == CbasRunSetStatus.CANCELING) {
        // Check how many runs in the run set are canceled
        Map<CbasRunStatus, RunDao.StatusCountRecord> canceledRunSetRuns =
            runDao.getRunStatusCounts(
                new RunDao.RunsFilters(
                    rs.runSetId(), Collections.singleton(CbasRunStatus.CANCELED)));

        // If the total number of canceled runs is the same as the number of runs in the run set,
        // then the entire run set is canceled.
        if (canceledRunSetRuns.values().size() == rs.runCount()) {
          runSetDao.updateStateAndRunSetDetails(
              rs.runSetId(),
              CbasRunSetStatus.CANCELED,
              rs.runCount(),
              rs.errorCount(),
              OffsetDateTime.now());
        } else {
          logger.info(
              "Cancellation of RunSet %s is still in progress. %d of %d runs have been canceled so far."
                  .formatted(rs.runSetId(), canceledRunSetRuns.values().size(), rs.runCount()));
        }
      }

      if (newStatusAndCounts.status != rs.status()
          || !Objects.equals(newStatusAndCounts.runErrors, rs.errorCount())
          || !Objects.equals(newStatusAndCounts.totalRuns, rs.runCount())) {
        // Update and re-fetch:
        runSetDao.updateStateAndRunSetDetails(
            rs.runSetId(),
            newStatusAndCounts.status(),
            newStatusAndCounts.totalRuns(),
            newStatusAndCounts.runErrors(),
            newStatusAndCounts.lastModified);
      } else {
        runSetDao.updateLastPolled(List.of(rs.runSetId()));
      }
      successBoolean = true;
      return runSetDao.getRunSet(rs.runSetId());
    } finally {
      micrometerMetrics.recordMethodCompletion(methodStartSample, successBoolean);
    }
  }

  private record StatusAndCounts(
      CbasRunSetStatus status, Integer totalRuns, Integer runErrors, OffsetDateTime lastModified) {}

  private StatusAndCounts newStatusAndErrorCounts(RunSet rs) {
    Map<CbasRunStatus, RunDao.StatusCountRecord> runStatusRecords =
        runDao.getRunStatusCounts(new RunDao.RunsFilters(rs.runSetId(), null));

    Map<CbasRunStatus, Integer> runStatusCounts =
        runStatusRecords.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().count()));

    OffsetDateTime lastModified =
        runStatusRecords.values().stream()
            .map(RunDao.StatusCountRecord::lastModified)
            .reduce(null, this::chooseLater);

    return new StatusAndCounts(
        CbasRunSetStatus.fromRunStatuses(runStatusCounts),
        runStatusCounts.values().stream().mapToInt(Integer::intValue).sum(),
        runStatusCounts.getOrDefault(CbasRunStatus.SYSTEM_ERROR, 0)
            + runStatusCounts.getOrDefault(CbasRunStatus.EXECUTOR_ERROR, 0),
        lastModified);
  }

  private OffsetDateTime chooseLater(OffsetDateTime a, OffsetDateTime b) {
    if (a == null) {
      return b;
    } else if (b == null) {
      return a;
    } else {
      return a.isAfter(b) ? a : b;
    }
  }
}
