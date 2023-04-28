package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.SystemStatus;
import bio.terra.cbas.model.SystemStatusSystems;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {

  private final WdsService wdsService;
  private final CromwellService cromwellService;
  private final LeonardoService leonardoService;

  @Autowired
  public PublicApiController(
      CromwellService cromwellService, WdsService wdsService, LeonardoService leonardoService) {
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
    this.leonardoService = leonardoService;
  }

  @Override
  public ResponseEntity<SystemStatus> getStatus() {
    SystemStatus result = new SystemStatus().ok(true);

    Map<String, HealthCheck> healthCheckSystems =
        Map.of(
            "cromwell", cromwellService,
            "wds", wdsService,
            "leonardo", leonardoService);

    healthCheckSystems.forEach(
        (serviceName, healthCheck) -> {
          var health = healthCheck.checkHealth();
          result.putSystemsItem(
              serviceName,
              new SystemStatusSystems().ok(health.isOk()).addMessagesItem(health.message()));
        });

    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
