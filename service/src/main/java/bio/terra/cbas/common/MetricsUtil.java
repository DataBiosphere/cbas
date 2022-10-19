package bio.terra.cbas.common;

import io.opencensus.common.Scope;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.BucketBoundaries;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class MetricsUtil {
  private MetricsUtil() {}

  public static final String METRICS_PREFIX = "terra/cbas/";

  public static final String OUTBOUND_REQUEST_METRICS = METRICS_PREFIX + "request/outbound";
  public static final String RUNS_SUBMITTED_SUCCESSFULLY_PER_RUN_SET_METRICS =
      OUTBOUND_REQUEST_METRICS + "/runs-submitted-successfully-per-run-set";
  public static final String INBOUND_REQUEST_METRICS = METRICS_PREFIX + "request/inbound";
  public static final String RECORDS_PER_REQUEST_METRICS =
      INBOUND_REQUEST_METRICS + "/records-per-request";
  public static final String METHOD_METRICS = METRICS_PREFIX + "method";
  public static final String EVENT_METRICS = METRICS_PREFIX + "event";

  public static final Measure.MeasureDouble M_METHOD_DURATION_MS =
      Measure.MeasureDouble.create(METHOD_METRICS, "Duration of method runs", "ms");

  public static final Measure.MeasureDouble M_OUTBOUND_REQUEST_DURATION_MS =
      Measure.MeasureDouble.create(OUTBOUND_REQUEST_METRICS, "Duration of outbound requests", "ms");

  public static final Measure.MeasureLong M_EVENT_COUNT =
      Measure.MeasureLong.create(EVENT_METRICS, "Counter for various events", "occurrences");

  public static final Measure.MeasureLong M_RECORDS_PER_REQUEST =
      Measure.MeasureLong.create(
          RECORDS_PER_REQUEST_METRICS, "Number of record IDs per request", "records-per-request");

  public static final Measure.MeasureLong M_RUNS_SUBMITTED_SUCCESSFULLY_PER_RUN_SET =
      Measure.MeasureLong.create(
          RUNS_SUBMITTED_SUCCESSFULLY_PER_RUN_SET_METRICS,
          "Number of Runs submitted successfully per Run Set",
          "runs-submitted-successfully-per-run-set");

  public static final TagKey TAGKEY_NAME = TagKey.create("name");
  public static final TagKey TAGKEY_STATUS = TagKey.create("status");

  public enum OutcomeStatus {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE");

    private final String outcomeStatus;

    OutcomeStatus(String outcomeStatus) {
      this.outcomeStatus = outcomeStatus;
    }

    public static OutcomeStatus ofSuccessBoolean(boolean successBoolean) {
      if (successBoolean) {
        return SUCCESS;
      } else {
        return FAILURE;
      }
    }

    @Override
    public String toString() {
      return outcomeStatus;
    }
  }

  private static final Tagger tagger = Tags.getTagger();
  private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();

  public static void recordTaggedStat(Map<TagKey, Object> tags, MeasureDouble md, Double d) {
    TagContextBuilder builder = tagger.emptyBuilder();

    for (var x : tags.entrySet()) {
      builder.putLocal(x.getKey(), TagValue.create(x.getValue().toString()));
    }
    TagContext tctx = builder.build();

    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(md, d).record();
    }
  }

  public static void recordTaggedStat(Map<TagKey, Object> tags, MeasureLong ml, Long l) {
    TagContextBuilder builder = tagger.emptyBuilder();

    for (var x : tags.entrySet()) {
      builder.putLocal(x.getKey(), TagValue.create(x.getValue().toString()));
    }
    TagContext tctx = builder.build();

    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(ml, l).record();
    }
  }

  public static void recordMethodCompletion(long startTimeNs, boolean successBoolean) {
    recordTaggedStat(
        Map.of(
            TAGKEY_NAME, loggableMethodName(1L),
            TAGKEY_STATUS, OutcomeStatus.ofSuccessBoolean(successBoolean)),
        M_METHOD_DURATION_MS,
        sinceInMilliseconds(startTimeNs));
  }

  public static void recordOutboundApiRequestCompletion(
      String apiName, long startTimeNs, boolean successBoolean) {
    recordTaggedStat(
        Map.of(TAGKEY_NAME, apiName, TAGKEY_STATUS, OutcomeStatus.ofSuccessBoolean(successBoolean)),
        M_OUTBOUND_REQUEST_DURATION_MS,
        sinceInMilliseconds(startTimeNs));
  }

  public static void incrementEventCounter(String eventName) {
    increaseEventCounter(eventName, 1L);
  }

  public static void recordRecordsInRequest(long numRecordIds) {
    recordTaggedStat(Map.of(), M_RECORDS_PER_REQUEST, numRecordIds);
  }

  public static void increaseEventCounter(String eventName, long count) {
    recordTaggedStat(Map.of(TAGKEY_NAME, eventName), M_EVENT_COUNT, count);
  }

  public static double sinceInMilliseconds(long startTimeNs) {
    return ((double) (System.nanoTime() - startTimeNs)) / 1e6;
  }

  public static void recordRunsSubmittedPerRunSet(long runsSubmitted) {
    recordTaggedStat(Map.of(), M_RUNS_SUBMITTED_SUCCESSFULLY_PER_RUN_SET, runsSubmitted);
  }

  /**
   * Traverses the call stack to find the calling method, and generate a suitable value for the
   * TAGKEY_NAME tag.
   *
   * @param extraStackDepth How many additional stack frames to go down before identifying the
   *     method. 0 is the direct caller's method name. 1 would be the caller's caller, and so on.
   * @return The name of the calling method.
   */
  public static String loggableMethodName(long extraStackDepth) {
    StackWalker.StackFrame caller =
        StackWalker.getInstance()
            .walk(stream -> stream.skip(1L + extraStackDepth).findFirst().get());

    return String.format("%s.%s", caller.getClassName(), caller.getMethodName());
  }

  public static void registerAllViews() {
    // Defining the distribution aggregations
    Aggregation methodTimeDistribution =
        Aggregation.Distribution.create(
            BucketBoundaries.create(
                Arrays.asList(
                    // Values in ms:
                    0.0,
                    25.0,
                    50.0,
                    75.0,
                    100.0,
                    250.0,
                    500.0,
                    750.0,
                    1000.0,
                    2500.0,
                    5000.0,
                    7500.0,
                    10000.0,
                    25000.0,
                    50000.0)));

    Aggregation recordsPerRequestDistribution =
        Aggregation.Distribution.create(
            BucketBoundaries.create(Arrays.asList(0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 100.0)));

    // Define the views
    View[] views =
        new View[] {
          View.create(
              View.Name.create(METHOD_METRICS),
              "The distribution of method timings",
              M_METHOD_DURATION_MS,
              methodTimeDistribution,
              List.of(TAGKEY_NAME, TAGKEY_STATUS)),
          View.create(
              View.Name.create(OUTBOUND_REQUEST_METRICS),
              "Distribution of outbound request timings",
              M_OUTBOUND_REQUEST_DURATION_MS,
              methodTimeDistribution,
              List.of(TAGKEY_NAME, TAGKEY_STATUS)),
          View.create(
              View.Name.create(EVENT_METRICS),
              "Counter for internal system events",
              M_EVENT_COUNT,
              Aggregation.Count.create(),
              List.of(TAGKEY_NAME)),
          View.create(
              View.Name.create(RECORDS_PER_REQUEST_METRICS),
              "Stats related to inbound requests",
              M_RECORDS_PER_REQUEST,
              recordsPerRequestDistribution,
              List.of()),
          View.create(
              View.Name.create(RUNS_SUBMITTED_SUCCESSFULLY_PER_RUN_SET_METRICS),
              "Stats related to outbound requests",
              M_RUNS_SUBMITTED_SUCCESSFULLY_PER_RUN_SET,
              recordsPerRequestDistribution,
              List.of())
        };

    // Create the view manager
    ViewManager vmgr = Stats.getViewManager();

    // Then finally register the views
    for (View view : views) vmgr.registerView(view);
  }
}
