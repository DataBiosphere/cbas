package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {

  @Override
  public ResponseEntity<RunStateResponse> postRun(String workflowUrl, Object workflowParams) {
    String runId = UUID.randomUUID().toString();
    return new ResponseEntity<>(
        new RunStateResponse().runId(runId).state(RunState.QUEUED), HttpStatus.CREATED);
  }
}
