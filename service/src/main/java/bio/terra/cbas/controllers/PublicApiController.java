package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.dependencies.common.HealthCheckable;
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

  @Autowired
  public PublicApiController(CromwellService cromwellService, WdsService wdsService) {
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
  }

  @Override
  public ResponseEntity<SystemStatus> getStatus() {
    Map<String, HealthCheckable.HealthCheckResult> checkResults =
        Map.of("cromwell", cromwellService, "wds", wdsService).entrySet().stream()
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
