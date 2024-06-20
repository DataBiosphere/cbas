package bio.terra.cbas.common;

import bio.terra.cbas.models.CbasRunStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    counter.increment();
  }

  public Timer.Sample startTimer() {
    return Timer.start(meterRegistry);
  }

  public void stopTimer(Timer.Sample sample, String name, String... tags) {
    sample.stop(meterRegistry.timer(name, tags));
  }
}
