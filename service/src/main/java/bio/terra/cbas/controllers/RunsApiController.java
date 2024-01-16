package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.common.exceptions.InvalidStatusTypeException;
import bio.terra.cbas.common.exceptions.MissingRunOutputsException;
import bio.terra.cbas.common.exceptions.RunNotFoundException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.model.RunResultsRequest;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import bio.terra.cbas.runsets.results.RunCompletionHandler;
import bio.terra.cbas.runsets.results.RunCompletionResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunsApiController implements RunsApi {
  private final SmartRunsPoller smartPoller;
  private final SamService samService;
  private final RunDao runDao;
  private final RunCompletionHandler runCompletionHandler;
  private final MeterRegistry meterRegistry;

  public RunsApiController(
      RunDao runDao,
      SmartRunsPoller smartPoller,
      SamService samService,
      RunCompletionHandler runCompletionHandler,
      MeterRegistry meterRegistry) {
    this.runDao = runDao;
    this.smartPoller = smartPoller;
    this.samService = samService;
    this.runCompletionHandler = runCompletionHandler;
    this.meterRegistry = meterRegistry;
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
    // validate request
    UUID engineId = body.getWorkflowId();
    CbasRunStatus resultsStatus = CbasRunStatus.fromCromwellStatus(body.getState().toString());
    List<String> failures = body.getFailures();

    Counter counter = Counter.builder("run_completion")
        .tag("completion_trigger", "callback")
        .tag("status", resultsStatus.toString()).register(meterRegistry);
    counter.increment();

    log.info(
        "Processing workflow callback for run ID %s with status %s."
            .formatted(engineId, resultsStatus));

    if (!resultsStatus.isTerminal()) {
      // only terminal status can be reported
      throw new InvalidStatusTypeException(
          "Results can not be posted for a non-terminal workflow state %s."
              .formatted(resultsStatus));
    }

    // lookup runID in database
    Optional<Run> runRecord =
        runDao.getRuns(new RunDao.RunsFilters(null, null, engineId.toString())).stream()
            .findFirst();
    if (runRecord.isEmpty()) {
      throw new RunNotFoundException(
          "Workflow ID with engine ID %s is not found.".formatted(engineId));
    }

    if (resultsStatus == CbasRunStatus.COMPLETE && body.getOutputs() == null) {
      throw new MissingRunOutputsException(
          "Outputs are required for a successfully completed workflow ID %s.".formatted(engineId));
    }

    // validate user permission
    // check if current user has write permissions on the workspace
    if (!samService.hasWritePermission()) {
      // This is a corner case when user got kicked off write permission while running a workflow.
      // Nor this service, nor API caller can remediate the situation.
      // Therefore, we update Status in database to SYSTEM_ERROR and return OK response.
      resultsStatus = CbasRunStatus.SYSTEM_ERROR;
      if (failures == null) {
        failures = new ArrayList<>();
      }
      failures.add(
          "User does not have write permission required to update workflow ID %s."
              .formatted(engineId));
    }

    // perform workflow completion work
    RunCompletionResult result =
        runCompletionHandler.updateResults(
            runRecord.get(), resultsStatus, body.getOutputs(), failures);
    return new ResponseEntity<>(result.toHttpStatus());
  }
}
