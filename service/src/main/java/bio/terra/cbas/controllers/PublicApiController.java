package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.model.SystemStatus;
import bio.terra.cbas.model.SystemStatusSystems;
import com.google.common.collect.ImmutableList;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {

  private final CromwellServerConfiguration cromwellConfig;
  private final WdsServerConfiguration wdsServerConfiguration;

  private record HealthcheckableService(String name, String healthcheckUrl) {
    HealthcheckedService result(Boolean isOk, Optional<String> healthcheckMessage) {
      return new HealthcheckedService(this, isOk, healthcheckMessage);
    }
  }

  private record HealthcheckedService(
      HealthcheckableService service, Boolean isOk, Optional<String> healthcheckMessage) {}

  @Autowired
  public PublicApiController(
      CromwellServerConfiguration cromwellConfig, WdsServerConfiguration wdsServerConfiguration) {
    this.cromwellConfig = cromwellConfig;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  @Override
  public ResponseEntity<SystemStatus> getStatus() {

    List<HealthcheckableService> servicesToCheck =
        ImmutableList.of(
            new HealthcheckableService("Cromwell", cromwellConfig.healthUri()),
            new HealthcheckableService("WDS", wdsServerConfiguration.healthcheckUri()));

    Stream<HealthcheckedService> checkResults =
        servicesToCheck.stream()
            .map(
                serviceToCheck -> {
                  try {
                    URL url = new URL(serviceToCheck.healthcheckUrl);
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    try (var stream = connection.getInputStream()) {
                      return serviceToCheck.result(true, Optional.empty());
                    }
                  } catch (Exception e) {
                    return serviceToCheck.result(false, Optional.of(e.getLocalizedMessage()));
                  }
                });

    SystemStatus result = new SystemStatus().ok(true);
    checkResults.forEach(
        checkResult -> {
          SystemStatusSystems system = new SystemStatusSystems();
          system.setOk(checkResult.isOk());
          checkResult.healthcheckMessage.ifPresent(system::addMessagesItem);
          result.putSystemsItem(checkResult.service.name, system);
        });

    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
