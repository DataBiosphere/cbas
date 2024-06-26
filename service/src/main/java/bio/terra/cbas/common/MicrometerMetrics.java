package bio.terra.cbas.common;

import static bio.terra.cbas.common.JavaMethodUtil.loggableMethodName;

import bio.terra.cbas.models.CbasRunStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetrics {

  private final MeterRegistry meterRegistry;

  private final String STATUS_TAG = "status";
  private final String RESPONSE_CODE_TAG = "response_code";

  public MicrometerMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /* ******** Counter metrics ******** */

  public void recordRunCallback(CbasRunStatus resultsStatus) {
    recordCounterMetric(
        "run_callback", List.of(new ImmutableTag(STATUS_TAG, resultsStatus.toString())), 1);
  }

  public void recordRunStatusUpdate(CbasRunStatus resultsStatus) {
    recordCounterMetric(
        "run_smartpoller_update", List.of(new ImmutableTag(STATUS_TAG, resultsStatus.toString())), 1);
  }

  public void increaseEventCounter(String metricName, long count) {
    recordCounterMetric(metricName, Collections.emptyList(), count);
  }

  /* ******** Event distribution metrics ******** */

  public void recordEventDistributionMetric(String metricName, int count) {
    recordEventDistributionMetric(metricName, Collections.emptyList(), count);
  }

  public void recordRunsSubmittedPerRunSet(UUID runSetId, long runsCount) {
    recordEventDistributionMetric(
        "runs_submitted_successfully_per_run_set",
        List.of(new ImmutableTag("run_set_id", runSetId.toString())),
        runsCount);
  }

  /* ******** Timer metrics ******** */

  public void recordPostMethodHandlerCompletion(
      Timer.Sample sample, String source, int responseCode) {
    recordTimerMetric(
        sample,
        "post_method_response_timing",
        List.of(
            new ImmutableTag("source", source),
            new ImmutableTag(RESPONSE_CODE_TAG, String.valueOf(responseCode))));
  }

  public void recordPostRunSetHandlerCompletion(
      Timer.Sample sample, int inputsCount, int outputsCount, int recordsCount, int responseCode) {
    recordTimerMetric(
        sample,
        "post_runset_response_timing",
        List.of(
            new ImmutableTag("inputs_count", String.valueOf(inputsCount)),
            new ImmutableTag("outputs_count", String.valueOf(outputsCount)),
            new ImmutableTag("records_count", String.valueOf(recordsCount)),
            new ImmutableTag(RESPONSE_CODE_TAG, String.valueOf(responseCode))));
  }

  public void recordMethodCompletion(Timer.Sample sample, boolean successBoolean) {
    recordTimerMetric(
        sample,
        loggableMethodName(1L),
        List.of(new ImmutableTag(STATUS_TAG, String.valueOf(successBoolean))));
  }

  public void recordOutboundApiRequestCompletion(
      Timer.Sample sample, String apiName, boolean successBoolean) {
    recordTimerMetric(
        sample, apiName, List.of(new ImmutableTag(STATUS_TAG, String.valueOf(successBoolean))));
  }

  public void stopTimer(Timer.Sample sample, String name, String... tags) {
    sample.stop(meterRegistry.timer(name, tags));
  }

  /* ******** Helper methods ******** */

  public Timer.Sample startTimer() {
    return Timer.start(meterRegistry);
  }

  private void recordCounterMetric(String metricName, List<Tag> tags, long count) {
    Counter counter = Counter.builder(metricName).tags(tags).register(meterRegistry);
    counter.increment(count);
  }

  private void recordEventDistributionMetric(String metricName, List<Tag> tags, long count) {
    DistributionSummary summary =
        DistributionSummary.builder(metricName).tags(tags).register(meterRegistry);
    summary.record(count);
  }

  private void recordTimerMetric(Timer.Sample sample, String name, List<Tag> tags) {
    sample.stop(meterRegistry.timer(name, tags));
  }
}
