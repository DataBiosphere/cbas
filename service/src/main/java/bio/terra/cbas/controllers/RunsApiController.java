package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import cromwell.client.ApiClient;
import cromwell.client.ApiException;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import cromwell.client.api.WorkflowsApi;
import cromwell.client.model.WorkflowQueryResponse;
import cromwell.client.model.WorkflowQueryResult;
import java.time.OffsetDateTime;
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

  private RunState convertToRunState(String workflowStatus) {
    return switch (workflowStatus) {
      case "On Hold" -> RunState.PAUSED;
      case "Submitted" -> RunState.QUEUED;
      case "Running" -> RunState.RUNNING;
      case "Aborting" -> RunState.CANCELING;
      case "Aborted" -> RunState.CANCELED;
      case "Succeeded" -> RunState.COMPLETE;
      case "Failed" -> RunState.EXECUTOR_ERROR;
      default -> RunState.UNKNOWN;
    };
  }

  private Date convertToDate(OffsetDateTime submissionTimestamp) {
    if (submissionTimestamp != null) {
      return new Date(submissionTimestamp.toInstant().toEpochMilli());
    }

    return null;
  }

  private RunLog convertToRunLog(WorkflowQueryResult queryResult) {
    // Note: Cromwell's /query endpoint doesn't return 'workflowUrl' or 'workflowInputs' hence
    // Setting it 'null' for now
    return new RunLog()
        .runId(queryResult.getId())
        .state(convertToRunState(queryResult.getStatus()))
        .workflowUrl(null)
        .name(queryResult.getName())
        .workflowParams(null)
        .submissionDate(convertToDate(queryResult.getSubmission()));
  }

  @Autowired
  public RunsApiController(CromwellServerConfiguration cromwellConfig) {
    this.cromwellConfig = cromwellConfig;
  }

  @Override
  public ResponseEntity<RunLogResponse> getRuns() {
    ApiClient client = new ApiClient();
    client.setBasePath(this.cromwellConfig.baseUri());
    WorkflowsApi workflowsApi = new WorkflowsApi(client);

    try {
      WorkflowQueryResponse queryResponse =
          workflowsApi.queryGet(
              "v1", null, null, null, null, null, null, null, null, null, null, null, null);

      List<RunLog> runsList =
          queryResponse.getResults().stream()
              .map(queryResult -> convertToRunLog(queryResult))
              .toList();

      return new ResponseEntity<>(new RunLogResponse().runs(runsList), HttpStatus.OK);
    } catch (cromwell.client.ApiException e) {
      System.out.println(e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public ResponseEntity<RunStateResponse> postRun(String workflowUrl, Object workflowParams) {

    ApiClient client = new ApiClient();
    client.setBasePath(this.cromwellConfig.baseUri());
    Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesApi =
        new Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi(client);
    String runId = UUID.randomUUID().toString();

    try {
      wesApi.runWorkflow(workflowParams.toString(), null, null, null, null, workflowUrl, null);

      return new ResponseEntity<>(
          new RunStateResponse().runId(runId).state(RunState.QUEUED), HttpStatus.CREATED);
    } catch (ApiException e) {
      System.out.println(e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
