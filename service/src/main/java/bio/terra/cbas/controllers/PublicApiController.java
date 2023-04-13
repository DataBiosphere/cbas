package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.dependencies.common.HealthCheckable;
import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.SystemStatus;
import bio.terra.cbas.model.SystemStatusSystems;
import bio.terra.cbas.util.Pair;
import java.util.Map;
import java.util.stream.Collectors;
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
    Map<String, HealthCheckable> healthCheckableSystems =
        Map.of(
            "cromwell", cromwellService,
            "wds", wdsService,
            "leonardo", leonardoService);

    Map<String, HealthCheckable.HealthCheckResult> checkResults =
        healthCheckableSystems.entrySet().stream()
            .map(
                serviceToCheckEntry -> {
                  String serviceName = serviceToCheckEntry.getKey();
                  HealthCheckable serviceCheckable = serviceToCheckEntry.getValue();
                  return new Pair<>(serviceName, serviceCheckable.checkHealth());
                })
            .collect(Collectors.toMap(Pair::a, Pair::b));

    SystemStatus result = new SystemStatus().ok(true);
    checkResults.forEach(
        (key, value) ->
            result.putSystemsItem(
                key, new SystemStatusSystems().ok(value.isOk()).addMessagesItem(value.message())));

    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
