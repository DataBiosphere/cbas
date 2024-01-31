package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import java.time.OffsetDateTime;
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
  private CbasApiConfiguration cbasApiConfiguration;

  @BeforeEach
  void init() {
    smartRunsPoller = mock(SmartRunsPoller.class);
    runDao = mock(RunDao.class);
    runSetDao = mock(RunSetDao.class);
    cbasApiConfiguration = mock(CbasApiConfiguration.class);
    when(cbasApiConfiguration.getMaxSmartPollRunSetUpdateSeconds()).thenReturn(1);
    when(cbasApiConfiguration.getMaxSmartPollRunUpdateSeconds()).thenReturn(1);
    abortManager = mock(RunSetAbortManager.class);
  }

  @Test
  void respectPollTimeLimit() {
    SmartRunSetsPoller smartRunSetsPoller =
        new SmartRunSetsPoller(smartRunsPoller, runSetDao, runDao, cbasApiConfiguration);

    UUID runSetId1 = UUID.randomUUID();
    RunSet runSetToUpdate1 =
        new RunSet(
            runSetId1,
            null,
            null,
            null,
            false,
            false,
            CbasRunSetStatus.RUNNING,
            null,
            null,
            OffsetDateTime.now().minusMinutes(10),
            1,
            0,
            null,
            null,
            null,
            null);

    UUID runSetId2 = UUID.randomUUID();
    RunSet runSetToUpdate2 =
        new RunSet(
            runSetId2,
            null,
            null,
            null,
            true,
            false,
            CbasRunSetStatus.RUNNING,
            null,
            null,
            OffsetDateTime.now().minusMinutes(3),
            1,
            0,
            null,
            null,
            null,
            null);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete =
        new Run(runId1, null, runSetToUpdate1, null, null, RUNNING, null, null, null);

    when(runDao.getRuns(new RunDao.RunsFilters(runSetId1, CbasRunStatus.NON_TERMINAL_STATES)))
        .thenReturn(List.of(run1Incomplete));

    when(smartRunsPoller.updateRuns(eq(List.of(run1Incomplete)), any()))
        .thenAnswer(
            i -> {
              Thread.sleep(1000);
              return new TimeLimitedUpdater.UpdateResult<Run>(List.of(run1Incomplete), 1, 1, true);
            });

    when(runSetDao.updateLastPolled(List.of(runSetId1))).thenReturn(1);
    when(runSetDao.getRunSet(runSetId1)).thenReturn(runSetToUpdate1);

    // Run the update method and check results (note: run sets are out of order, to test last-polled
    // ordering):
    TimeLimitedUpdater.UpdateResult<RunSet> updateResult =
        smartRunSetsPoller.updateRunSets(List.of(runSetToUpdate2, runSetToUpdate1));
    assertEquals(
        updateResult.updatedList().stream().map(RunSet::runSetId).toList(),
        List.of(runSetId2, runSetId1));
    assertEquals(updateResult.totalEligible(), 2);
    assertEquals(updateResult.totalUpdated(), 1);
    assertFalse(updateResult.fullyUpdated());

    // Verify that the second run set never gets touched:
    verify(runDao, never()).getRuns(new RunDao.RunsFilters(runSetId2, NON_TERMINAL_STATES));
    verify(runSetDao, never()).updateLastPolled(List.of(runSetId2));
  }

  @Test
  void updateRunSetToComplete() {
    SmartRunSetsPoller smartRunSetsPoller =
        new SmartRunSetsPoller(smartRunsPoller, runSetDao, runDao, cbasApiConfiguration);

    UUID runSetId = UUID.randomUUID();
    RunSet runSetToUpdate =
        new RunSet(
            runSetId,
            null,
            null,
            null,
            false,
            false,
            CbasRunSetStatus.RUNNING,
            null,
            null,
            null,
            2,
            0,
            null,
            null,
            null,
            null);

    RunSet runSetUpdated =
        new RunSet(
            runSetId,
            null,
            null,
            null,
            true,
            false,
            CbasRunSetStatus.COMPLETE,
            null,
            null,
            null,
            2,
            0,
            null,
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
        .thenReturn(
            new TimeLimitedUpdater.UpdateResult<>(List.of(run1Complete, run2Complete), 2, 2, true));

    OffsetDateTime lastModified = OffsetDateTime.now();

    // When we re-query for up-to-the-minute run status counts:
    ArgumentCaptor<RunDao.RunsFilters> runsFiltersForGetRunStatusCounts =
        ArgumentCaptor.forClass(RunDao.RunsFilters.class);
    when(runDao.getRunStatusCounts(runsFiltersForGetRunStatusCounts.capture()))
        .thenReturn(Map.of(COMPLETE, new RunDao.StatusCountRecord(COMPLETE, 2, lastModified)));

    // Updating the run set with the new information:
    when(runSetDao.updateStateAndRunDetails(
            runSetId, CbasRunSetStatus.COMPLETE, 2, 0, lastModified))
        .thenReturn(1);

    // Re-fetching the updated run set for update:
    when(runSetDao.getRunSet(runSetId)).thenReturn(runSetUpdated);

    // Run the update:
    var result = smartRunSetsPoller.updateRunSets(List.of(runSetToUpdate));

    // Validate the results:
    verify(runDao).getRuns(any());
    assertEquals(runSetId, runsFiltersForGetRuns.getValue().runSetId());
    assertEquals(NON_TERMINAL_STATES, runsFiltersForGetRuns.getValue().statuses());

    verify(smartRunsPoller).updateRuns(eq(List.of(run1Incomplete, run2Incomplete)), any());

    verify(runDao).getRunStatusCounts(any());
    assertEquals(runSetId, runsFiltersForGetRunStatusCounts.getValue().runSetId());
    assertNull(runsFiltersForGetRunStatusCounts.getValue().statuses());

    verify(runSetDao)
        .updateStateAndRunDetails(runSetId, CbasRunSetStatus.COMPLETE, 2, 0, lastModified);
    verify(runSetDao).getRunSet(runSetId);

    assertEquals(List.of(runSetUpdated), result.updatedList());
  }

  @Test
  void updateLastPolledTimestampAnyway() {
    SmartRunSetsPoller smartRunSetsPoller =
        new SmartRunSetsPoller(smartRunsPoller, runSetDao, runDao, cbasApiConfiguration);

    UUID runSetId = UUID.randomUUID();
    RunSet runSetToUpdate =
        new RunSet(
            runSetId,
            null,
            null,
            null,
            false,
            false,
            CbasRunSetStatus.RUNNING,
            null,
            null,
            null,
            1,
            0,
            null,
            null,
            null,
            null);

    RunSet runSetTimestampUpdated =
        new RunSet(
            runSetId,
            null,
            null,
            null,
            false,
            false,
            CbasRunSetStatus.RUNNING,
            null,
            null,
            OffsetDateTime.now(),
            1,
            0,
            null,
            null,
            null,
            null);

    UUID runId1 = UUID.randomUUID();
    Run run1 = new Run(runId1, null, runSetToUpdate, null, null, RUNNING, null, null, null);

    // Set up mocks:

    // Initial query of runs in the run set:
    ArgumentCaptor<RunDao.RunsFilters> runsFiltersForGetRuns =
        ArgumentCaptor.forClass(RunDao.RunsFilters.class);
    when(runDao.getRuns(runsFiltersForGetRuns.capture())).thenReturn(List.of(run1));

    // When the smart runs poller is checked:
    when(smartRunsPoller.updateRuns(List.of(run1)))
        .thenReturn(new TimeLimitedUpdater.UpdateResult<>(List.of(run1), 1, 1, true));

    // When we re-query for up-to-the-minute run status counts:
    ArgumentCaptor<RunDao.RunsFilters> runsFiltersForGetRunStatusCounts =
        ArgumentCaptor.forClass(RunDao.RunsFilters.class);
    when(runDao.getRunStatusCounts(runsFiltersForGetRunStatusCounts.capture()))
        .thenReturn(
            Map.of(RUNNING, new RunDao.StatusCountRecord(RUNNING, 1, OffsetDateTime.now())));

    // Updating the run set with the new information:
    when(runSetDao.updateLastPolled(List.of(runSetId))).thenReturn(1);

    // Re-fetching the updated run set for update:
    when(runSetDao.getRunSet(runSetId)).thenReturn(runSetTimestampUpdated);

    // Run the update:
    var result = smartRunSetsPoller.updateRunSets(List.of(runSetToUpdate));

    // Validate the results:
    verify(runDao).getRuns(any());
    assertEquals(runSetId, runsFiltersForGetRuns.getValue().runSetId());
    assertEquals(NON_TERMINAL_STATES, runsFiltersForGetRuns.getValue().statuses());

    verify(smartRunsPoller).updateRuns(eq(List.of(run1)), any());

    verify(runDao).getRunStatusCounts(any());
    assertEquals(runSetId, runsFiltersForGetRunStatusCounts.getValue().runSetId());
    assertNull(runsFiltersForGetRunStatusCounts.getValue().statuses());

    verify(runSetDao).updateLastPolled(List.of(runSetId));
    verify(runSetDao).getRunSet(runSetId);

    assertEquals(List.of(runSetTimestampUpdated), result.updatedList());
  }
}
