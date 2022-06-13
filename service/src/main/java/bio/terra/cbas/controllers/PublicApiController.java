package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.model.SystemStatus;
import bio.terra.cbas.model.SystemStatusSystems;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {

  private final CromwellServerConfiguration cromwellConfig;

  @Autowired
  public PublicApiController(CromwellServerConfiguration cromwellConfig) {
    this.cromwellConfig = cromwellConfig;
  }

  @Override
  public ResponseEntity<SystemStatus> getStatus() {

    String result;
    boolean isOk;
    try {
      URL url = new URL(this.cromwellConfig.healthUri());
      URLConnection connection = url.openConnection();
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      try (var stream = connection.getInputStream()) {
        result = new String(stream.readAllBytes());
        isOk = true;
      } catch (Exception e) {
        result = e.getLocalizedMessage();
        isOk = false;
      }
    } catch (Exception e) {
      result = e.getLocalizedMessage();
      isOk = false;
    }

    return new ResponseEntity<>(
        new SystemStatus()
            .ok(true)
            .putSystemsItem("Cromwell", new SystemStatusSystems().ok(isOk).addMessagesItem(result)),
        HttpStatus.OK);
  }
}
