package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SmartRunsPoller.class)
public class TestSmartRunSetsPoller {

  private SmartRunsPoller smartRunsPoller;
  private RunDao runDao;
  private RunSetDao runSetDao;

  @BeforeEach
  void init() {
    smartRunsPoller = mock(SmartRunsPoller.class);
    runDao = mock(RunDao.class);
    runSetDao = mock(RunSetDao.class);
  }

  @Test
  void updateRunSetToComplete() {
    SmartRunSetsPoller smartRunSetsPoller =
        new SmartRunSetsPoller(smartRunsPoller, runSetDao, runDao);

    UUID runSetId = UUID.randomUUID();
    RunSet runSetToUpdate =
        new RunSet(
            runSetId,
            null,
            null,
            null,
            false,
            CbasRunSetStatus.RUNNING,
            null,
            null,
            null,
            2,
            0,
            null,
            null,
            null);

    RunSet runSetUpdated =
        new RunSet(
            runSetId,
            null,
            null,
            null,
            false,
            CbasRunSetStatus.COMPLETE,
            null,
            null,
            null,
            2,
            0,
            null,
            null,
            null);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete =
        new Run(runId1, null, runSetToUpdate, null, null, RUNNING, null, null, null);

    Run run1Complete =
        new Run(runId1, null, runSetToUpdate, null, null, COMPLETE, null, null, null);

    UUID runId2 = UUID.randomUUID();
    Run run2Incomplete =
        new Run(runId2, null, runSetToUpdate, null, null, RUNNING, null, null, null);

    Run run2Complete =
        new Run(runId2, null, runSetToUpdate, null, null, COMPLETE, null, null, null);

    // Set up mocks:

    // Initial query of runs in the run set:
    ArgumentCaptor<RunDao.RunsFilters> runsFiltersForGetRuns =
        ArgumentCaptor.forClass(RunDao.RunsFilters.class);
    when(runDao.getRuns(runsFiltersForGetRuns.capture()))
        .thenReturn(List.of(run1Incomplete, run2Incomplete));

    // When the smart runs poller is checked:
    when(smartRunsPoller.updateRuns(List.of(run1Incomplete, run2Incomplete)))
        .thenReturn(List.of(run1Complete, run2Complete));

    // When we re-query for up-to-the-minute run status counts:
    ArgumentCaptor<RunDao.RunsFilters> runsFiltersForGetRunStatusCounts =
        ArgumentCaptor.forClass(RunDao.RunsFilters.class);
    when(runDao.getRunStatusCounts(runsFiltersForGetRunStatusCounts.capture()))
        .thenReturn(Map.of(COMPLETE, 2));

    // Updating the run set with the new information:
    when(runSetDao.updateStateAndRunDetails(runSetId, CbasRunSetStatus.COMPLETE, 2, 0))
        .thenReturn(1);

    // Re-fetching the updated run set for update:
    when(runSetDao.getRunSet(runSetId)).thenReturn(runSetUpdated);

    // Run the update:
    var result = smartRunSetsPoller.updateRunSets(List.of(runSetToUpdate));

    // Validate the results:
    verify(runDao).getRuns(any());
    assertEquals(runSetId, runsFiltersForGetRuns.getValue().runSetId());
    assertEquals(NON_TERMINAL_STATES, runsFiltersForGetRuns.getValue().statuses());

    verify(smartRunsPoller).updateRuns(List.of(run1Incomplete, run2Incomplete));

    verify(runDao).getRunStatusCounts(any());
    assertEquals(runSetId, runsFiltersForGetRunStatusCounts.getValue().runSetId());
    assertNull(runsFiltersForGetRunStatusCounts.getValue().statuses());

    verify(runSetDao).updateStateAndRunDetails(runSetId, CbasRunSetStatus.COMPLETE, 2, 0);
    verify(runSetDao).getRunSet(runSetId);

    assertEquals(List.of(runSetUpdated), result);
  }
}
