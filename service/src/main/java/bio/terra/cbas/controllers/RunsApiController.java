package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.Run;
import cromwell.client.ApiClient;
import cromwell.client.ApiException;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {
  private final CromwellServerConfiguration cromwellConfig;
  private final RunDao runDao;

  public RunsApiController(CromwellServerConfiguration cromwellConfig, RunDao runDao) {
    this.cromwellConfig = cromwellConfig;
    this.runDao = runDao;
  }

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

  public Date convertToDate(OffsetDateTime submissionTimestamp) {
    if (submissionTimestamp != null) {
      return Date.from(submissionTimestamp.toInstant());
    }

    return null;
  }

  private RunLog runToRunLog(Run run) {

    System.out.println(run);

    System.out.println(
        new RunLog()
            .runId(run.id().toString())
            .workflowUrl(run.runSet().method().methodUrl())
            .submissionDate(convertToDate(run.submissionTimestamp())));

    return new RunLog()
        .runId(run.id().toString())
        .workflowUrl(run.runSet().method().methodUrl())
        .submissionDate(convertToDate(run.submissionTimestamp()));
  }

  @Override
  public ResponseEntity<RunLogResponse> getRuns() {

    var queryResults = runDao.retrieve();
    // System.out.println(queryResults);
    List<RunLog> runsList = queryResults.stream().map(this::runToRunLog).toList();
    System.out.println(runsList);
    System.out.println(new RunLogResponse().runs(runsList));

    return new ResponseEntity<>(new RunLogResponse().runs(runsList), HttpStatus.OK);
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
