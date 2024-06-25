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

  public MicrometerMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /* ******** Counter metrics ******** */

  public void recordRunCallback(CbasRunStatus resultsStatus) {
    recordCounterMetric(
        "run_callback", List.of(new ImmutableTag("status", resultsStatus.toString())), 1);
  }

  public void recordRunStatusUpdate(CbasRunStatus resultsStatus) {
    recordCounterMetric(
        "run_smartpoller_update", List.of(new ImmutableTag("status", resultsStatus.toString())), 1);
  }

  public void increaseEventCounter(String metricName, long count) {
    recordCounterMetric(metricName, Collections.emptyList(), count);
  }

  //  public void incrementSuccessfulFileParse(String scheme, String fromType) {
  //    recordCounterMetric("file_parse_success", List.of(new ImmutableTag("scheme",
  // Optional.ofNullable(scheme).orElse("/")), new ImmutableTag("from_type", fromType)));
  //  }
  //
  //  public void incrementUnsuccessfulFileParse(Object badValue) {
  //    recordCounterMetric("file_parse_failure", List.of(new ImmutableTag("from_type",
  // badValue.getClass().getSimpleName())));
  //  }

  /* ******** Event distribution metrics ******** */

  // TODO: should this be refactored more?

  public void logRecordIdsInSubmission(int recordIdsSize) {
    recordEventDistributionMetric("records_per_request", Collections.emptyList(), recordIdsSize);
  }

  public void logInputsInSubmission(int inputsCount) {
    recordEventDistributionMetric("inputs_per_request", Collections.emptyList(), inputsCount);
  }

  public void logOutputsInSubmission(int outputsCount) {
    recordEventDistributionMetric("outputs_per_request", Collections.emptyList(), outputsCount);
  }

  public void logRunsSubmittedPerRunSet(UUID runSetId, long runsCount) {
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
        "post_method_completion",
        List.of(
            new ImmutableTag("source", source),
            new ImmutableTag("response_code", String.valueOf(responseCode))));
  }

  public void recordMethodCompletion(Timer.Sample sample, boolean successBoolean) {
    recordTimerMetric(
        sample,
        loggableMethodName(1L),
        List.of(new ImmutableTag("status", String.valueOf(successBoolean))));
  }

  public void recordOutboundApiRequestCompletion(
      Timer.Sample sample, String apiName, boolean successBoolean) {
    recordTimerMetric(
        sample, apiName, List.of(new ImmutableTag("status", String.valueOf(successBoolean))));
  }

  /* ******** Helper methods ******** */

  private void recordCounterMetric(String metricName, List<Tag> tags, long count) {
    Counter counter = Counter.builder(metricName).tags(tags).register(meterRegistry);
    counter.increment(count);
  }

  private void recordEventDistributionMetric(String metricName, List<Tag> tags, long count) {
    DistributionSummary summary =
        DistributionSummary.builder(metricName).tags(tags).register(meterRegistry);
    summary.record(count);
  }

  // TODO: this method can be removed when Michael's PR is merged
  public Timer.Sample startTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordTimerMetric(Timer.Sample sample, String name, List<Tag> tags) {
    sample.stop(meterRegistry.timer(name, tags));
  }
}
