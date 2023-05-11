package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.api.RunSetsApi.log;
import static bio.terra.cbas.models.CbasRunStatus.NON_TERMINAL_STATES;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RunSetAbortManager {

  private final RunSetDao runSetDao;
  private final RunDao runDao;
  private final CromwellService cromwellService;

  public RunSetAbortManager(RunSetDao runSetDao, RunDao runDao, CromwellService cromwellService) {
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.cromwellService = cromwellService;
  }

  public Map<String, List<String>> abortRunSet(UUID runSetId) {

    List<String> failedRunIds = new ArrayList<>();

    // Get the run set associated with runSetId
    RunSet runSet = runSetDao.getRunSet(runSetId);

    // Update the run set to have a canceling state
    runSetDao.updateStateAndRunDetails(
        runSetId,
        CbasRunSetStatus.CANCELING,
        runSet.runCount(),
        runSet.errorCount(),
        OffsetDateTime.now());

    // Get a list of workflows able to be canceled
    List<Run> runningWorkflows =
        runDao.getRuns(new RunDao.RunsFilters(runSetId, NON_TERMINAL_STATES));
    List<String> submittedAbortWorkflows = new ArrayList<>();

    for (Run run : runningWorkflows) {
      // Trying inside the for-loop in case a single run fails to be updated
      try {
        cromwellService.cancelRun(run);
        submittedAbortWorkflows.add(run.runId().toString());
      } catch (cromwell.client.ApiException e) {
        String msg = "Unable to abort workflow %s.".formatted(run.runId());
        log.error(msg, e);
        failedRunIds.add(run.runId().toString());
        // Add the error message against the run
        run.withErrorMessage(msg);
      }
    }

    Map<String, List<String>> failedAndSubmittedIds = new HashMap<>();
    failedAndSubmittedIds.put("Failed run IDs", failedRunIds);
    failedAndSubmittedIds.put("Submitted Abort Workflows", submittedAbortWorkflows);

    return failedAndSubmittedIds;
  }
}
