package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.EXECUTOR_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.gson.Gson;
import cromwell.client.model.FailureMessage;
import cromwell.client.model.RunLog;
import cromwell.client.model.RunStatus;
import cromwell.client.model.State;
import cromwell.client.model.WorkflowMetadataResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SmartRunsPoller.class)
public class TestSmartRunsPoller {

  private static final OffsetDateTime runSubmittedTime = OffsetDateTime.now();
  private static final UUID runningRunId1 = UUID.randomUUID();
  private static final String runningRunEngineId1 = UUID.randomUUID().toString();
  private static final String runningRunEntityId1 = UUID.randomUUID().toString();
  private static final OffsetDateTime runningRunStatusUpdateTime = OffsetDateTime.now();
  private static final UUID runningRunId2 = UUID.randomUUID();
  private static final String runningRunEngineId2 = UUID.randomUUID().toString();
  private static final String runningRunEntityId2 = UUID.randomUUID().toString();

  private static final UUID runningRunId3 = UUID.randomUUID();
  private static final String runningRunEngineId3 = UUID.randomUUID().toString();
  private static final String runningRunEntityId3 = UUID.randomUUID().toString();

  private static final UUID completedRunId = UUID.randomUUID();
  private static final String completedRunEngineId = UUID.randomUUID().toString();
  private static final String completedRunEntityId = UUID.randomUUID().toString();
  private static final OffsetDateTime completedRunStatusUpdateTime = OffsetDateTime.now();
  private static final String errorMessagesNull = null;

  public ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .setDateFormat(new StdDateFormat())
          .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);

  private CromwellService cromwellService;
  private RunDao runsDao;
  private WdsService wdsService;
  private SmartRunsPoller smartRunsPoller;

  static String outputDefinition =
      """
        [
          {
            "output_name": "wf_hello.hello.salutation",
            "output_type": { "type": "primitive", "primitive_type": "String" },
            "record_attribute": "foo_name"
          }
        ]
      """;
  private static final RunSet runSet =
      new RunSet(
          UUID.randomUUID(),
          new Method(
              UUID.randomUUID(), "methodurl", "inputdefinition", outputDefinition, "entitytype"));

  final Run runToUpdate1 =
      new Run(
          runningRunId1,
          runningRunEngineId1,
          runSet,
          runningRunEntityId1,
          runSubmittedTime,
          RUNNING,
          runningRunStatusUpdateTime,
          runningRunStatusUpdateTime,
          errorMessagesNull);

  final Run runToUpdate2 =
      new Run(
          runningRunId2,
          runningRunEngineId2,
          runSet,
          runningRunEntityId2,
          runSubmittedTime,
          RUNNING,
          runningRunStatusUpdateTime,
          runningRunStatusUpdateTime,
          errorMessagesNull);

  final Run runAlreadyCompleted =
      new Run(
          completedRunId,
          completedRunEngineId,
          runSet,
          completedRunEntityId,
          runSubmittedTime,
          COMPLETE,
          completedRunStatusUpdateTime,
          completedRunStatusUpdateTime,
          errorMessagesNull);
  final Run runToUpdate3 =
      new Run(
          runningRunId3,
          runningRunEngineId3,
          runSet,
          runningRunEntityId3,
          runSubmittedTime,
          UNKNOWN,
          runningRunStatusUpdateTime,
          runningRunStatusUpdateTime,
          errorMessagesNull);

  @BeforeEach
  public void init() {
    cromwellService = mock(CromwellService.class);
    runsDao = mock(RunDao.class);
    wdsService = mock(WdsService.class);
    smartRunsPoller = new SmartRunsPoller(cromwellService, runsDao, wdsService, objectMapper);
  }

  @Test
  void pollRunningRuns() throws Exception {
    when(cromwellService.runStatus(eq(runningRunEngineId1)))
        .thenReturn(new RunStatus().runId(runningRunEngineId1).state(State.RUNNING));

    when(runsDao.updateLastPolledTimestamp(eq(runToUpdate1.id()))).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService).runStatus(eq(runningRunEngineId1));
    verify(runsDao).updateLastPolledTimestamp(eq(runToUpdate1.id()));
    verify(cromwellService, never()).runStatus(eq(completedRunEngineId));
    verify(runsDao, never()).updateLastPolledTimestamp(eq(runAlreadyCompleted.id()));

    assertEquals(2, actual.size());
    assertEquals(
        RUNNING,
        actual.stream().filter(r -> r.id().equals(runningRunId1)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(completedRunId)).toList().get(0).status());
  }

  @Test
  void updateNewlyCompletedRuns() throws Exception {
    when(cromwellService.runStatus(eq(runningRunEngineId1)))
        .thenReturn(new RunStatus().runId(runningRunEngineId1).state(State.COMPLETE));

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
    when(cromwellService.getOutputs(eq(runningRunEngineId1))).thenReturn(parseRunLog.getOutputs());

    when(runsDao.updateRunStatus(eq(runToUpdate1.id()), eq(COMPLETE))).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService)
        .runStatus(eq(runningRunEngineId1)); // (verify that the workflow is in a failed state)
    verify(runsDao)
        .updateRunStatus(
            eq(runToUpdate1.id()),
            eq(COMPLETE)); // (verify that error messages are recieved from Cromwell)
    verify(wdsService)
        .updateRecord(
            eq(mockRequest),
            eq(runToUpdate1.runSet().method().recordType()),
            eq(runToUpdate1.recordId()));

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(eq(runAlreadyCompleted.id()), eq(COMPLETE));

    assertEquals(2, actual.size());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(runningRunId1)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(completedRunId)).toList().get(0).status());
  }

  @Test
  void updatingOutputFails() throws Exception {
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();

    // output name is purposely misspelled so that attaching output fails
    String misspelledRawOutputs =
        """
        {
            "outputs": {
              "wf_hello.hello.salutationz": "Hello batch!"
            }
          }
        """;

    Object misspelledCromwellOutputs =
        object.fromJson(misspelledRawOutputs, RunLog.class).getOutputs();

    String rawOutputs =
        """
        {
            "outputs": {
              "wf_hello.hello.salutation": "Hello batch!"
            }
          }
        """;
    Object cromwellOutputs = object.fromJson(rawOutputs, RunLog.class).getOutputs();

    RecordAttributes mockAttributes = new RecordAttributes();
    mockAttributes.put("foo_name", "Hello batch!");
    RecordRequest mockRequest = new RecordRequest().attributes(mockAttributes);

    when(cromwellService.getOutputs(eq(runningRunEngineId1))).thenReturn(misspelledCromwellOutputs);
    when(cromwellService.getOutputs(eq(runningRunEngineId2))).thenReturn(cromwellOutputs);
    when(cromwellService.runStatus(eq(runningRunEngineId1)))
        .thenReturn(new RunStatus().runId(runningRunEngineId1).state(State.COMPLETE));
    when(cromwellService.runStatus(eq(runningRunEngineId2)))
        .thenReturn(new RunStatus().runId(runningRunEngineId2).state(State.COMPLETE));
    when(runsDao.updateRunStatus(eq(runToUpdate1.id()), eq(SYSTEM_ERROR))).thenReturn(1);
    when(runsDao.updateRunStatus(eq(runToUpdate2.id()), eq(COMPLETE))).thenReturn(1);

    var actual =
        smartRunsPoller.updateRuns(List.of(runToUpdate1, runToUpdate2, runAlreadyCompleted));

    // verify that Run is marked as Failed as attaching outputs failed
    verify(cromwellService).runStatus(eq(runningRunEngineId1));
    verify(runsDao).updateRunStatus(eq(runToUpdate1.id()), eq(SYSTEM_ERROR));
    verify(wdsService, never())
        .updateRecord(
            eq(mockRequest),
            eq(runToUpdate1.runSet().method().recordType()),
            eq(runToUpdate1.recordId()));

    // verify that second Run whose status could be updated has been updated
    verify(cromwellService).runStatus(eq(runningRunEngineId2));
    verify(runsDao).updateRunStatus(eq(runToUpdate2.id()), eq(COMPLETE));
    verify(wdsService)
        .updateRecord(
            eq(mockRequest),
            eq(runToUpdate2.runSet().method().recordType()),
            eq(runToUpdate2.recordId()));

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(eq(runAlreadyCompleted.id()), eq(COMPLETE));

    assertEquals(3, actual.size());
    assertEquals(
        SYSTEM_ERROR,
        actual.stream().filter(r -> r.id().equals(runningRunId1)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(runningRunId2)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.id().equals(completedRunId)).toList().get(0).status());
  }

  @Test
  void databaseUpdatedWithCromwellError() throws Exception {

    String cromwellError =
        """
        {
        "failures": [
            {
              "causedBy": [
                {
                  "causedBy": [],
                  "message": "Required workflow input 'wf_hello.hello.addressee' not specified"
                }
              ],
              "message": "Workflow input processing failed"
            }
          ]
          }
          """;

    List<FailureMessage> listOfFails =
        objectMapper.readValue(cromwellError, WorkflowMetadataResponse.class).getFailures();
    String cromwellErrorMessage = CromwellService.getErrorMessage(listOfFails);

    when(cromwellService.runStatus(eq(runningRunEngineId3)))
        .thenReturn(new RunStatus().runId(runningRunEngineId3).state(State.EXECUTOR_ERROR));
    when(cromwellService.getRunErrors(eq(runToUpdate3))).thenReturn(cromwellErrorMessage);
    when(runsDao.updateErrorMessage(eq(runToUpdate3.id()), eq(cromwellErrorMessage))).thenReturn(1);
    when(runsDao.updateRunStatus(eq(runToUpdate3.id()), eq(EXECUTOR_ERROR))).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate3));

    verify(cromwellService)
        .runStatus(eq(runningRunEngineId3)); // (verify that the workflow is in a failed state)
    verify(runsDao).updateRunStatus(eq(runToUpdate3.id()), eq(EXECUTOR_ERROR));
    verify(runsDao)
        .updateErrorMessage(
            eq(runToUpdate3.id()),
            eq(cromwellErrorMessage)); // (verify that error messages are received from Cromwell)

    assertEquals(
        EXECUTOR_ERROR,
        actual.stream().filter(r -> r.id().equals(runningRunId3)).toList().get(0).status());

    assertEquals(
        "Workflow input processing failed (Required workflow input 'wf_hello.hello.addressee' not specified)",
        actual.stream().filter(r -> r.id().equals(runningRunId3)).toList().get(0).errorMessages());
  }

  @Test
  void hasOutputDefinitionReturnsTrue() throws JsonProcessingException {
    assertTrue(smartRunsPoller.hasOutputDefinition(runToUpdate1));
  }

  //  @Test
  //  void hasOutputDefinitionReturnsFalse() throws JsonProcessingException {
  //    String outputDefinition = "[]";
  //    RunSet runSet =
  //        new RunSet(
  //            UUID.randomUUID(),
  //            new Method(
  //                UUID.randomUUID(), "methodurl", "inputdefinition", outputDefinition,
  // "entitytype"));
  //    Run run =
  //        new Run(
  //            runningRunId1,
  //            runningRunEngineId1,
  //            runSet,
  //            runningRunEntityId1,
  //            runSubmittedTime,
  //            RUNNING,
  //            runningRunStatusUpdateTime,
  //            runningRunStatusUpdateTime,
  //            errorMessages);
  //
  //    assertFalse(smartRunsPoller.hasOutputDefinition(run));
  //
  //  }
}
