package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.dependencies.common.ScheduledHealthChecker;
import bio.terra.cbas.model.SystemStatus;
import bio.terra.cbas.model.SystemStatusSystems;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {

  private final ScheduledHealthChecker scheduledHealthChecker;

  @Autowired
  public PublicApiController(ScheduledHealthChecker scheduledHealthChecker) {
    this.scheduledHealthChecker = scheduledHealthChecker;
  }

  @Override
  public ResponseEntity<SystemStatus> getStatus() {
    SystemStatus result = new SystemStatus().ok(true);

    scheduledHealthChecker
        .getHealthCheckStatuses()
        .forEach(
            (serviceName, healthCheck) ->
              result.putSystemsItem(
                  serviceName,
                  new SystemStatusSystems()
                      .ok(healthCheck.isOk())
                      .addMessagesItem(healthCheck.message())));

    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
