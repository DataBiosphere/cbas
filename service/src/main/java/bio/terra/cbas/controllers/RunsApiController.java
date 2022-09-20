package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.models.Run;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {
  private final RunDao runDao;

  public RunsApiController(RunDao runDao) {
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

  private Date convertToDate(OffsetDateTime submissionTimestamp) {
    if (submissionTimestamp != null) {
      return new Date(submissionTimestamp.toInstant().toEpochMilli());
    }

    return null;
  }

  private RunLog runToRunLog(Run run) {

    return new RunLog()
        .runId(run.id().toString())
        .workflowUrl(run.runSet().method().methodUrl())
        .name(null)
        .state(convertToRunState(run.status()))
        .workflowParams(run.runSet().method().inputDefinition())
        .submissionDate(convertToDate(run.submissionTimestamp()));
  }

  @Override
  public ResponseEntity<RunLogResponse> getRuns() {

    var queryResults = runDao.retrieve();
    List<RunLog> runsList = queryResults.stream().map(this::runToRunLog).toList();
    return new ResponseEntity<>(new RunLogResponse().runs(runsList), HttpStatus.OK);
  }
}
