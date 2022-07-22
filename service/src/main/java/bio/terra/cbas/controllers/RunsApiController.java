package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.model.LogRunRequest;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import cromwell.client.ApiClient;
import cromwell.client.api.WorkflowsApi;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {

  private final CromwellServerConfiguration cromwellConfig;

  @Autowired
  public RunsApiController(CromwellServerConfiguration cromwellConfig) {
    this.cromwellConfig = cromwellConfig;
  }

  @Override
  public ResponseEntity<RunLogResponse> getRun() {
    List<LogRunRequest> runs = new ArrayList<>();
    String runId = UUID.randomUUID().toString();
    String name = "CBAS";
    Date now = new Date();

    runs.add(
        new LogRunRequest()
            .runId(runId)
            .state(RunState.UNKNOWN)
            .workflowUrl("urlHere")
            .name(name)
            .workflowParams("params")
            .submissionDate(now));

    return ResponseEntity.ok(new RunLogResponse().runs(runs));
  }

  @Override
  public ResponseEntity<RunStateResponse> postRun(String workflowUrl, Object workflowParams) {

    ApiClient client = new ApiClient();
    client.setBasePath(this.cromwellConfig.baseUri());
    WorkflowsApi workflowsApi = new WorkflowsApi(client);
    String runId = UUID.randomUUID().toString();

    ResponseEntity result;

    try {
      workflowsApi.submit(
          "v1",
          null,
          workflowUrl,
          null,
          workflowParams.toString(),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);

      result =
          new ResponseEntity<>(
              new RunStateResponse().runId(runId).state(RunState.QUEUED), HttpStatus.CREATED);
    } catch (cromwell.client.ApiException e) {
      System.out.println(e);
      result =
          new ResponseEntity<>(
              new RunStateResponse().runId(runId).state(RunState.SYSTEM_ERROR),
              HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return result;
  }
}
