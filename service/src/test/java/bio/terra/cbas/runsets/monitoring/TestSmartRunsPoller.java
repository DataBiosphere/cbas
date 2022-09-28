package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import cromwell.client.ApiException;
import cromwell.client.model.RunStatus;
import cromwell.client.model.State;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SmartRunsPoller.class)
public class TestSmartRunsPoller {

  private static final UUID runningRunId = UUID.randomUUID();
  private static final String runningRunEngineId = UUID.randomUUID().toString();
  private static final String runningRunEntityId = UUID.randomUUID().toString();
  private static final OffsetDateTime runningRunSubmittedtime = OffsetDateTime.now();

  private static final UUID completedRunId = UUID.randomUUID();
  private static final String completedRunEngineId = UUID.randomUUID().toString();
  private static final String completedRunEntityId = UUID.randomUUID().toString();
  private static final OffsetDateTime completedRunSubmittedtime = OffsetDateTime.now();

  private static final RunSet runSet =
      new RunSet(
          UUID.randomUUID(),
          new Method(UUID.randomUUID(), "methodurl", "inputdefinition", "entitytype"));

  final Run runToUpdate =
      new Run(
          runningRunId,
          runningRunEngineId,
          runSet,
          runningRunEntityId,
          runningRunSubmittedtime,
          RUNNING);

  final Run runAlreadyCompleted =
      new Run(
          completedRunId,
          completedRunEngineId,
          runSet,
          completedRunEntityId,
          completedRunSubmittedtime,
          COMPLETE);

  @Test
  void pollRunningRuns() throws ApiException {
    CromwellService cromwellService = mock(CromwellService.class);
    RunDao runsDao = mock(RunDao.class);
    SmartRunsPoller smartRunsPoller = new SmartRunsPoller(cromwellService, runsDao);

    when(cromwellService.runStatus(eq(runningRunEngineId)))
        .thenReturn(new RunStatus().runId(runningRunEngineId).state(State.RUNNING));

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate, runAlreadyCompleted));

    verify(cromwellService).runStatus(eq(runningRunEngineId));
    verify(cromwellService, never()).runStatus(eq(completedRunEngineId));

    assertEquals(2, actual.size());
    assertEquals(
        RUNNING, actual.stream().filter(r -> r.id().equals(runningRunId)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(completedRunId)).toList().get(0).status());
  }

  @Test
  void updateNewlyCompletedRuns() throws ApiException {
    CromwellService cromwellService = mock(CromwellService.class);
    RunDao runsDao = mock(RunDao.class);
    SmartRunsPoller smartRunsPoller = new SmartRunsPoller(cromwellService, runsDao);

    when(cromwellService.runStatus(eq(runningRunEngineId)))
        .thenReturn(new RunStatus().runId(runningRunEngineId).state(State.COMPLETE));

    when(runsDao.updateRunStatus(eq(runToUpdate), eq(COMPLETE))).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate, runAlreadyCompleted));

    verify(cromwellService).runStatus(eq(runningRunEngineId));
    verify(runsDao).updateRunStatus(eq(runToUpdate), eq(COMPLETE));

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(eq(runAlreadyCompleted), eq(COMPLETE));

    assertEquals(2, actual.size());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(runningRunId)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(completedRunId)).toList().get(0).status());
  }
}
