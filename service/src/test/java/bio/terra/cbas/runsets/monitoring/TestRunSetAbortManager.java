package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.CANCELING;
import static bio.terra.cbas.models.CbasRunStatus.NON_TERMINAL_STATES;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import cromwell.client.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = {RunSetAbortManager.class})
class TestRunSetAbortManager {

  @MockBean private RunSetDao runSetDao;
  @MockBean private RunDao runDao;
  @MockBean private CromwellService cromwellService;
  private final UUID workspaceId = UUID.randomUUID();

  @Test
  void testRunSetAbort() throws ApiException {
    RunSetAbortManager runSetAbortManager =
        new RunSetAbortManager(runSetDao, runDao, cromwellService);

    UUID runSetId = UUID.randomUUID();
    UUID runId1 = UUID.randomUUID();
    UUID runId2 = UUID.randomUUID();

    RunSet runSetToBeCancelled =
        new RunSet(
            runSetId,
            null,
            "",
            "",
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
            null,
            workspaceId);

    Run run1Running =
        new Run(
            runId1,
            UUID.randomUUID().toString(),
            runSetToBeCancelled,
            null,
            null,
            RUNNING,
            null,
            null,
            null);

    Run run2Running =
        new Run(
            runId2,
            UUID.randomUUID().toString(),
            runSetToBeCancelled,
            null,
            null,
            RUNNING,
            null,
            null,
            null);

    when(runDao.createRun(run1Running)).thenReturn(1);
    when(runDao.createRun(run2Running)).thenReturn(1);

    List<Run> runs = new ArrayList<>();
    runs.add(run1Running);
    runs.add(run2Running);

    when(runSetDao.getRunSet(runSetId)).thenReturn(runSetToBeCancelled);

    when(runSetDao.updateStateAndRunDetails(
            eq(runSetId), eq(CbasRunSetStatus.CANCELING), eq(2), eq(0), any()))
        .thenReturn(1);

    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, NON_TERMINAL_STATES))).thenReturn(runs);

    when(runDao.getRuns(any())).thenReturn(runs);

    var result = runSetAbortManager.abortRunSet(runSetId);

    ArgumentCaptor<Run> newRunCaptor = ArgumentCaptor.forClass(Run.class);
    verify(cromwellService, times(2)).cancelRun(newRunCaptor.capture());
    List<Run> capturedRuns = newRunCaptor.getAllValues();
    assertEquals(2, capturedRuns.size());
    assertEquals(run1Running.runId(), capturedRuns.get(0).runId());
    assertEquals(run1Running.status(), capturedRuns.get(0).status());
    assertEquals(run1Running.engineId(), capturedRuns.get(0).engineId());

    assertEquals(run2Running.runId(), capturedRuns.get(1).runId());
    assertEquals(run2Running.status(), capturedRuns.get(1).status());
    assertEquals(run2Running.engineId(), capturedRuns.get(1).engineId());

    assertEquals(List.of(), result.getAbortRequestFailedIds());
    assertEquals(2, result.getAbortRequestSubmittedIds().size());
    assertEquals(runId1, result.getAbortRequestSubmittedIds().get(0));
    assertEquals(runId2, result.getAbortRequestSubmittedIds().get(1));
  }

  @Test
  void oneFailedOneSucceededRun() throws Exception {
    RunSetAbortManager runSetAbortManager =
        new RunSetAbortManager(runSetDao, runDao, cromwellService);

    UUID runSetId = UUID.randomUUID();
    UUID runId1 = UUID.randomUUID();
    UUID runId2 = UUID.randomUUID();

    RunSet runSetToBeCancelled =
        new RunSet(
            runSetId,
            null,
            "",
            "",
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
            null,
            workspaceId);

    Run run1Running =
        new Run(
            runId1,
            UUID.randomUUID().toString(),
            runSetToBeCancelled,
            null,
            null,
            RUNNING,
            null,
            null,
            null);

    Run run2Running =
        new Run(
            runId2,
            UUID.randomUUID().toString(),
            runSetToBeCancelled,
            null,
            null,
            RUNNING,
            null,
            null,
            null);

    when(runDao.createRun(run1Running)).thenReturn(1);
    when(runDao.createRun(run2Running)).thenReturn(1);

    List<Run> runs = new ArrayList<>();
    runs.add(run1Running);
    runs.add(run2Running);

    when(runSetDao.getRunSet(runSetId)).thenReturn(runSetToBeCancelled);
    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, NON_TERMINAL_STATES))).thenReturn(runs);
    when(runSetDao.updateStateAndRunDetails(
            eq(runSetId), eq(CbasRunSetStatus.CANCELING), eq(2), eq(0), any()))
        .thenReturn(1);

    doThrow(new cromwell.client.ApiException("Unable to abort workflow %s.".formatted(runId2)))
        .when(cromwellService)
        .cancelRun(run2Running);

    var result = runSetAbortManager.abortRunSet(runSetId);

    assertEquals(1, result.getAbortRequestFailedIds().size());
    assertEquals(1, result.getAbortRequestSubmittedIds().size());
    assertEquals(runId1, result.getAbortRequestSubmittedIds().get(0));
    assertEquals(runId2.toString(), result.getAbortRequestFailedIds().get(0));
    assertNotSame(CANCELING, run2Running.status());
  }
}
