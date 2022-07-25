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

    ApiClient client = new ApiClient();
    client.setBasePath(this.cromwellConfig.baseUri());
    WorkflowsApi workflowsApi = new WorkflowsApi(client);
    LogRunRequest metadata = new LogRunRequest();

    List<LogRunRequest> runs = new ArrayList<>();

    runs.add(
        new LogRunRequest()
            .runId(metadata.getRunId())
            .state(metadata.getState())
            .workflowUrl(metadata.getWorkflowUrl())
            .name(metadata.getName())
            .workflowParams(metadata.getWorkflowParams())
            .submissionDate(metadata.getSubmissionDate()));

    ResponseEntity result;

    try {
      workflowsApi.metadata("v1", metadata.getRunId(), null, null, null);

      result = new ResponseEntity<>(new RunLogResponse().runs(runs), HttpStatus.OK);
    } catch (cromwell.client.ApiException e) {
      result =
          new ResponseEntity<>(new RunLogResponse().runs(runs), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return result;
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
