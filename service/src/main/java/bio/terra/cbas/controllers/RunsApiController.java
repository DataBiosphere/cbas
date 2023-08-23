package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.model.RunResultsRequest;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.cbas.runsets.exceptions.RunResultsInvalidStatusException;
import bio.terra.cbas.runsets.monitoring.RunResultsManager;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {
  private final SmartRunsPoller smartPoller;
  private final SamService samService;
  private final RunDao runDao;
  private final RunResultsManager runResultsManager;

  public RunsApiController(
      RunDao runDao,
      SmartRunsPoller smartPoller,
      SamService samService,
      RunResultsManager runResultsManager) {
    this.runDao = runDao;
    this.smartPoller = smartPoller;
    this.samService = samService;
    this.runResultsManager = runResultsManager;
  }

  private RunLog runToRunLog(Run run) {

    return new RunLog()
        .runId(run.runId())
        .engineId(run.engineId())
        .runSetId(run.getRunSetId())
        .recordId(run.recordId())
        .workflowUrl(run.runSet().methodVersion().url())
        .name(null)
        .state(CbasRunStatus.toCbasApiState(run.status()))
        .workflowParams(run.runSet().inputDefinition())
        .workflowOutputs(run.runSet().outputDefinition())
        .submissionDate(DateUtils.convertToDate(run.submissionTimestamp()))
        .lastModifiedTimestamp(DateUtils.convertToDate(run.lastModifiedTimestamp()))
        .lastPolledTimestamp(DateUtils.convertToDate(run.lastPolledTimestamp()))
        .errorMessages(run.errorMessages());
  }

  @Override
  public ResponseEntity<RunLogResponse> getRuns(UUID runSetId) {
    // check if current user has read permissions on the workspace
    if (!samService.hasReadPermission()) {
      throw new ForbiddenException(SamService.READ_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    List<Run> queryResults = runDao.getRuns(new RunDao.RunsFilters(runSetId, null));
    UpdateResult<Run> updatedRunsResult = smartPoller.updateRuns(queryResults);

    List<RunLog> responseList =
        updatedRunsResult.updatedList().stream().map(this::runToRunLog).toList();
    return new ResponseEntity<>(
        new RunLogResponse().runs(responseList).fullyUpdated(updatedRunsResult.fullyUpdated()),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> postRunResults(RunResultsRequest body) {
    // validate user permission
    // check if current user has read permissions on the workspace
    if (!samService.hasWritePermission()) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    // validate request
    UUID runId = body.getWorkflowId();
    CbasRunStatus resultsStatus = CbasRunStatus.fromValue(body.getState());
    if (!resultsStatus.isTerminal()) {
      // only terminal status can be reported
      throw new RunResultsInvalidStatusException(
          String.format(
              "Results can not be posted for a non-terminal workflow state {}.", resultsStatus));
    }

    // perform workflow completion work
    runResultsManager.updateResults(runId, resultsStatus, body.getOutputs());
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
