package bio.terra.cbas.common;

import bio.terra.cbas.models.CbasRunStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetrics {

  private final MeterRegistry meterRegistry;

  public MicrometerMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void logRunCompletion(String completionTrigger, CbasRunStatus resultsStatus) {
    Counter counter =
        Counter.builder("run_completion")
            .tag("completion_trigger", completionTrigger)
            .tag("status", resultsStatus.toString())
            .register(meterRegistry);
    // This null check only triggers when running tests, where it's hard to mock out the
    // meterRegistry's internal .counter method without springboot 3's observability frameworks.
    // See https://github.com/DataBiosphere/terra-workspace-data-service/pull/461 (starting at
    // QuartzJobTest.scala for an example of what we might want to do when we upgrade)
    if (counter != null) {
      counter.increment();
    }
  }
}
