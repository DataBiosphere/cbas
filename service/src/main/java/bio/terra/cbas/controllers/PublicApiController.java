package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.model.SystemStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {

  @Override
  public ResponseEntity<SystemStatus> getStatus() {
    return new ResponseEntity<>(new SystemStatus().ok(true), HttpStatus.OK);
  }
}
