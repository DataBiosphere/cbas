package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.dependencies.common.ScheduledHealthChecker;
import bio.terra.cbas.model.CapabilitiesResponse;
import bio.terra.cbas.model.SystemStatus;
import bio.terra.cbas.model.SystemStatusSystems;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {

  private final ScheduledHealthChecker scheduledHealthChecker;
  private final CapabilitiesResponse capabilitiesResponse;

  @Autowired
  public PublicApiController(
      ScheduledHealthChecker scheduledHealthChecker, ObjectMapper objectMapper) {
    // read the "capabilities.json" file from resources and parse it into response object
    InputStream inputStream = getClass().getResourceAsStream("/capabilities.json");
    CapabilitiesResponse tempCapabilitiesResponse;
    try {
      tempCapabilitiesResponse = objectMapper.readValue(inputStream, CapabilitiesResponse.class);
      log.info("Capabilities in this CBAS version: {}", tempCapabilitiesResponse);
    } catch (Exception e) {
      log.error("Could not read 'capabilities.json' file. Error: {}", e.getMessage(), e);
      tempCapabilitiesResponse = null;
    }

    this.scheduledHealthChecker = scheduledHealthChecker;
    this.capabilitiesResponse = tempCapabilitiesResponse;
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

  @Override
  public ResponseEntity<CapabilitiesResponse> capabilities() {
    if (capabilitiesResponse != null) {
      return new ResponseEntity<>(capabilitiesResponse, HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
