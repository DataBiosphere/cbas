package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {
  private final SmartRunsPoller smartPoller;
  private final RunDao runDao;

  public RunsApiController(RunDao runDao, SmartRunsPoller smartPoller) {
    this.runDao = runDao;
    this.smartPoller = smartPoller;
  }

  private RunLog runToRunLog(Run run) {

    return new RunLog()
        .runId(run.run_id().toString())
        .engineId(run.engineId())
        .workflowUrl(run.runSet().method().methodSourceUrl())
        .name(null)
        .state(CbasRunStatus.toCbasApiState(run.status()))
        .workflowParams(run.runSet().inputDefinition())
        .workflowOutputs(run.runSet().outputDefinition())
        .submissionDate(DateUtils.convertToDate(run.submissionTimestamp()))
        .lastModifiedTimestamp(DateUtils.convertToDate(run.lastModifiedTimestamp()))
        .errorMessages(run.errorMessages());
  }

  @Override
  public ResponseEntity<RunLogResponse> getRuns(String runSetId) {

    List<Run> queryResults = runDao.getRuns(runSetId);
    List<Run> updatedRunResults = smartPoller.updateRuns(queryResults);

    List<RunLog> responseList = updatedRunResults.stream().map(this::runToRunLog).toList();
    return new ResponseEntity<>(new RunLogResponse().runs(responseList), HttpStatus.OK);
  }
}
