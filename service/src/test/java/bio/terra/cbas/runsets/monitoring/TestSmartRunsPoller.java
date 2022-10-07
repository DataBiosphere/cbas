package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.gson.Gson;
import cromwell.client.model.RunLog;
import cromwell.client.model.RunStatus;
import cromwell.client.model.State;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordRequest;
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

  public ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .setDateFormat(new StdDateFormat())
          .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);

  static String outputDefinition =
      """
        [
          {
            "output_name": "wf_hello.hello.salutation",
            "output_type": "String",
            "record_attribute": "foo_name"
          }
        ]
      """;
  private static final RunSet runSet =
      new RunSet(
          UUID.randomUUID(),
          new Method(
              UUID.randomUUID(), "methodurl", "inputdefinition", outputDefinition, "entitytype"));

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
  void pollRunningRuns() throws Exception {
    CromwellService cromwellService = mock(CromwellService.class);
    RunDao runsDao = mock(RunDao.class);
    WdsService wdsService = mock(WdsService.class);
    SmartRunsPoller smartRunsPoller =
        new SmartRunsPoller(cromwellService, runsDao, wdsService, null);

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
  void updateNewlyCompletedRuns() throws Exception {
    CromwellService cromwellService = mock(CromwellService.class);
    RunDao runsDao = mock(RunDao.class);
    WdsService wdsService = mock(WdsService.class);
    SmartRunsPoller smartRunsPoller =
        new SmartRunsPoller(cromwellService, runsDao, wdsService, objectMapper);

    when(cromwellService.runStatus(eq(runningRunEngineId)))
        .thenReturn(new RunStatus().runId(runningRunEngineId).state(State.COMPLETE));

    String runLogValue =
        """
        {
            "outputs": {
              "wf_hello.hello.salutation": "Hello batch!"
            },
            "request": {
              "tags": {},
              "workflow_engine_parameters": {},
              "workflow_params": {
                "wf_hello.hello.addressee": "batch"
              },
              "workflow_type": "None supplied",
              "workflow_type_version": "None supplied"
            },
            "run_id": "c38181fd-e4df-4fa2-b2ba-a71090b6d97c",
            "run_log": {
              "end_time": "2022-10-04T15:54:49.142Z",
              "name": "wf_hello",
              "start_time": "2022-10-04T15:54:32.280Z"
            },
            "state": "COMPLETE",
            "task_logs": [
              {
                "end_time": "2022-10-04T15:54:47.411Z",
                "exit_code": 0,
                "name": "wf_hello.hello",
                "start_time": "2022-10-04T15:54:33.837Z",
                "stderr": "/Users/kpierre/repos/cromwell/cromwell-executions/wf_hello/c38181fd-e4df-4fa2-b2ba-a71090b6d97c/call-hello/execution/stderr",
                "stdout": "/Users/kpierre/repos/cromwell/cromwell-executions/wf_hello/c38181fd-e4df-4fa2-b2ba-a71090b6d97c/call-hello/execution/stdout"
              }
            ]
          }
        """;

    RecordAttributes mockAttributes = new RecordAttributes();
    mockAttributes.put("foo_name", "Hello batch!");
    RecordRequest mockRequest = new RecordRequest().attributes(mockAttributes);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    RunLog parseRunLog = object.fromJson(runLogValue, RunLog.class);
    when(cromwellService.getOutputs(eq(runningRunEngineId))).thenReturn(parseRunLog.getOutputs());

    when(runsDao.updateRunStatus(eq(runToUpdate), eq(COMPLETE))).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate, runAlreadyCompleted));

    verify(cromwellService).runStatus(eq(runningRunEngineId));
    verify(runsDao).updateRunStatus(eq(runToUpdate), eq(COMPLETE));
    verify(wdsService)
        .updateRecord(
            eq(mockRequest),
            eq(runToUpdate.runSet().method().recordType()),
            eq(runToUpdate.recordId()));

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

  @Test
  void updatingOutputFails() throws Exception {
    CromwellService cromwellService = mock(CromwellService.class);
    RunDao runsDao = mock(RunDao.class);
    WdsService wdsService = mock(WdsService.class);
    SmartRunsPoller smartRunsPoller =
        new SmartRunsPoller(cromwellService, runsDao, wdsService, objectMapper);

    // output name is purposely misspelled so that attaching output fails
    String rawOutputs =
        """
        {
            "outputs": {
              "wf_hello.hello.salutationz": "Hello batch!"
            }
          }
        """;
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(rawOutputs, RunLog.class).getOutputs();

    RecordAttributes mockAttributes = new RecordAttributes();
    mockAttributes.put("foo_name", "Hello batch!");
    RecordRequest mockRequest = new RecordRequest().attributes(mockAttributes);

    when(cromwellService.getOutputs(eq(runningRunEngineId))).thenReturn(cromwellOutputs);
    when(cromwellService.runStatus(eq(runningRunEngineId)))
        .thenReturn(new RunStatus().runId(runningRunEngineId).state(State.COMPLETE));
    when(runsDao.updateRunStatus(eq(runToUpdate), eq(SYSTEM_ERROR))).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate, runAlreadyCompleted));

    // verify that Run is marked as Failed as attaching outputs failed
    verify(cromwellService).runStatus(eq(runningRunEngineId));
    verify(runsDao).updateRunStatus(eq(runToUpdate), eq(SYSTEM_ERROR));

    verify(wdsService, never())
        .updateRecord(
            eq(mockRequest),
            eq(runToUpdate.runSet().method().recordType()),
            eq(runToUpdate.recordId()));

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(eq(runAlreadyCompleted), eq(COMPLETE));

    assertEquals(2, actual.size());
    assertEquals(
        SYSTEM_ERROR,
        actual.stream().filter(r -> r.id().equals(runningRunId)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(completedRunId)).toList().get(0).status());
  }
}
