package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.exceptions.ExceptionUtils.getSamForbiddenExceptionMsg;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {
  private final SmartRunsPoller smartPoller;
  private final SamService samService;
  private final RunDao runDao;
  private static final Logger logger = LoggerFactory.getLogger(RunsApiController.class);

  public RunsApiController(RunDao runDao, SmartRunsPoller smartPoller, SamService samService) {
    this.runDao = runDao;
    this.smartPoller = smartPoller;
    this.samService = samService;
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
      logger.info(
          getSamForbiddenExceptionMsg(SamService.READ_ACTION, SamService.RESOURCE_TYPE_WORKSPACE));
      throw new ForbiddenException(SamService.RESOURCE_TYPE_WORKSPACE, SamService.READ_ACTION);
    }

    List<Run> queryResults = runDao.getRuns(new RunDao.RunsFilters(runSetId, null));
    UpdateResult<Run> updatedRunsResult = smartPoller.updateRuns(queryResults);

    List<RunLog> responseList =
        updatedRunsResult.updatedList().stream().map(this::runToRunLog).toList();
    return new ResponseEntity<>(
        new RunLogResponse().runs(responseList).fullyUpdated(updatedRunsResult.fullyUpdated()),
        HttpStatus.OK);
  }
}
