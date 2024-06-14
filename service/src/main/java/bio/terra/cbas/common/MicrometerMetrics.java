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

  public void logRunCallback(CbasRunStatus resultsStatus) {
    Counter counter =
        Counter.builder("run_callback")
            .tag("status", resultsStatus.toString())
            .register(meterRegistry);
    counter.increment();
  }

  public void logRunStatusUpdate(CbasRunStatus resultsStatus) {
    Counter counter =
        Counter.builder("run_smartpoller_update")
            .tag("status", resultsStatus.toString())
            .register(meterRegistry);
    counter.increment();
  }
}
