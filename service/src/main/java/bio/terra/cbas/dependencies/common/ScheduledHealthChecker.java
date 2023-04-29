package bio.terra.cbas.dependencies.common;

import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A spring scheduled component that periodically checks the health of the dependencies and updates
 * the cached statuses.
 */
@Component
public class ScheduledHealthChecker {

  private static final Logger logger = LoggerFactory.getLogger(ScheduledHealthChecker.class);

  private final Map<String, HealthCheck> healthCheckSystems;

  private final Map<String, HealthCheck.Result> healthCheckStatuses;

  @Autowired
  public ScheduledHealthChecker(
      CromwellService cromwellService, WdsService wdsService, LeonardoService leonardoService) {
    this.healthCheckStatuses = new ConcurrentHashMap<>();
    this.healthCheckSystems =
        Map.of(
            "cromwell", cromwellService,
            "wds", wdsService,
            "leonardo", leonardoService);
  }

  @Scheduled(fixedDelay = 5 * 60 * 1000)
  public void checkHealth() {
    healthCheckSystems.entrySet().stream()
        .parallel()
        .forEach(
            entry -> {
              var health = entry.getValue().checkHealth();
              healthCheckStatuses.put(entry.getKey(), health);
              logger.info("Health check for {} is {}", entry.getKey(), health.isOk());
            });
  }

  public Map<String, HealthCheck.Result> getHealthCheckStatuses() {
    return healthCheckStatuses;
  }
}
