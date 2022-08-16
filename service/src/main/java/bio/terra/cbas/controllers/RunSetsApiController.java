package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunSetsApiController implements RunSetsApi {

  private final CromwellServerConfiguration cromwellConfig;

  @Autowired
  public RunSetsApiController(CromwellServerConfiguration cromwellConfig) {
    this.cromwellConfig = cromwellConfig;
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {
    log.info(request.toString());
    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(UUID.randomUUID().toString())
            .addRunsItem(
                new RunStateResponse().runId(UUID.randomUUID().toString()).state(RunState.RUNNING))
            .state(RunSetState.RUNNING),
        HttpStatus.OK);
  }
}
