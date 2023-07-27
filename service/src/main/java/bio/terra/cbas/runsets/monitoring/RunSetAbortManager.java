package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.api.RunSetsApi.log;
import static bio.terra.cbas.models.CbasRunStatus.NON_TERMINAL_STATES;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

  public AbortRequestDetails abortRunSet(UUID runSetId) {
    AbortRequestDetails abortDetails = new AbortRequestDetails();

    List<String> failedRunIds = new ArrayList<>();

    // Get the run set associated with runSetId
    RunSet runSet = runSetDao.getRunSet(runSetId);

    // Update the run set to have a canceling state if not canceling
    if (runSet.status() != CbasRunSetStatus.CANCELING && runSet.status().nonTerminal()) {
      runSetDao.updateStateAndRunDetails(
          runSetId,
          CbasRunSetStatus.CANCELING,
          runSet.runCount(),
          runSet.errorCount(),
          OffsetDateTime.now());
    }

    // Get a list of workflows able to be canceled
    List<Run> runningWorkflows =
        runDao.getRuns(new RunDao.RunsFilters(runSetId, NON_TERMINAL_STATES));
    List<UUID> submittedAbortWorkflows = new ArrayList<>();

    for (Run run : runningWorkflows) {
      // Trying inside the for-loop in case a single run fails to be updated
      try {
        cromwellService.cancelRun(run);
        submittedAbortWorkflows.add(run.runId());
      } catch (cromwell.client.ApiException e) {
        String msg = "Unable to abort workflow %s.".formatted(run.runId());
        log.error(msg, e);
        failedRunIds.add(run.runId().toString());
        // Add the error message against the run
        run.withErrorMessage(msg);
      } catch (AzureAccessTokenException | DependencyNotAvailableException e) {
        throw new RuntimeException(e);
      }
    }

    abortDetails.setFailedIds(failedRunIds);
    abortDetails.setSubmittedIds(submittedAbortWorkflows);

    return abortDetails;
  }

  public static class AbortRequestDetails {

    private List<String> abortRequestFailedIds = null;

    private List<UUID> abortRequestSubmittedIds = null;

    public List<String> getAbortRequestFailedIds() {
      return abortRequestFailedIds;
    }

    public List<UUID> getAbortRequestSubmittedIds() {
      return abortRequestSubmittedIds;
    }

    public void setFailedIds(List<String> failedIds) {
      this.abortRequestFailedIds = failedIds;
    }

    public void setSubmittedIds(List<UUID> submittedIds) {
      this.abortRequestSubmittedIds = submittedIds;
    }
  }
}
