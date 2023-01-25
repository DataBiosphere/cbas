package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.EXECUTOR_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
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

  private static final OffsetDateTime methodCreatedTime = OffsetDateTime.now();
  private static final OffsetDateTime methodLastRunTime = OffsetDateTime.now();
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
  private static final String errorMessages = null;

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
            "destination": { "type": "record_update", "record_attribute": "foo_name" }
          }
        ]
      """;
  private static final UUID runSetId = UUID.randomUUID();
  private static final RunSet runSet =
      new RunSet(
          runSetId,
          new MethodVersion(
              UUID.randomUUID(),
              new Method(
                  UUID.randomUUID(),
                  "methodName",
                  "methodDescription",
                  methodCreatedTime,
                  runSetId,
                  "method source"),
              "version name",
              "version description",
              methodCreatedTime,
              runSetId,
              "file:///method/source/url"),
          "runSetName",
          "runSetDescription",
          false,
          CbasRunSetStatus.UNKNOWN,
          runSubmittedTime,
          runSubmittedTime,
          runSubmittedTime,
          0,
          0,
          "inputDefinition",
          outputDefinition,
          "entityType");

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
          errorMessages);

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
          errorMessages);

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
          errorMessages);
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
          errorMessages);

  @BeforeEach
  public void init() {
    cromwellService = mock(CromwellService.class);
    runsDao = mock(RunDao.class);
    wdsService = mock(WdsService.class);
    smartRunsPoller = new SmartRunsPoller(cromwellService, runsDao, wdsService, objectMapper);
  }

  @Test
  void pollRunningRuns() throws Exception {
    when(cromwellService.runStatus(runningRunEngineId1))
        .thenReturn(new RunStatus().runId(runningRunEngineId1).state(State.RUNNING));

    when(runsDao.updateLastPolledTimestamp(runToUpdate1.runId())).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService).runStatus(runningRunEngineId1);
    verify(runsDao).updateLastPolledTimestamp(runToUpdate1.runId());
    verify(cromwellService, never()).runStatus(completedRunEngineId);
    verify(runsDao, never()).updateLastPolledTimestamp(runAlreadyCompleted.runId());

    assertEquals(2, actual.size());
    assertEquals(
        RUNNING,
        actual.stream().filter(r -> r.runId().equals(runningRunId1)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.runId().equals(completedRunId)).toList().get(0).status());
  }

  @Test
  void updateNewlyCompletedRuns() throws Exception {
    when(cromwellService.runStatus(runningRunEngineId1))
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
    when(cromwellService.getOutputs(runningRunEngineId1)).thenReturn(parseRunLog.getOutputs());

    when(runsDao.updateRunStatus(runToUpdate1.runId(), COMPLETE)).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService)
        .runStatus(runningRunEngineId1); // (verify that the workflow is in a failed state)
    verify(runsDao)
        .updateRunStatus(
            runToUpdate1.runId(),
            COMPLETE); // (verify that error messages are received from Cromwell)
    verify(wdsService)
        .updateRecord(mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId());

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(runAlreadyCompleted.runId(), COMPLETE);

    assertEquals(2, actual.size());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.runId().equals(runningRunId1)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.runId().equals(completedRunId)).toList().get(0).status());
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

    when(cromwellService.getOutputs(runningRunEngineId1)).thenReturn(misspelledCromwellOutputs);
    when(cromwellService.getOutputs(runningRunEngineId2)).thenReturn(cromwellOutputs);
    when(cromwellService.runStatus(runningRunEngineId1))
        .thenReturn(new RunStatus().runId(runningRunEngineId1).state(State.COMPLETE));
    when(cromwellService.runStatus(runningRunEngineId2))
        .thenReturn(new RunStatus().runId(runningRunEngineId2).state(State.COMPLETE));
    when(runsDao.updateRunStatus(runToUpdate1.runId(), SYSTEM_ERROR)).thenReturn(1);
    when(runsDao.updateRunStatus(runToUpdate2.runId(), COMPLETE)).thenReturn(1);

    var actual =
        smartRunsPoller.updateRuns(List.of(runToUpdate1, runToUpdate2, runAlreadyCompleted));

    // verify that Run is marked as Failed as attaching outputs failed
    verify(cromwellService).runStatus(runningRunEngineId1);
    verify(runsDao).updateRunStatus(runToUpdate1.runId(), SYSTEM_ERROR);
    verify(wdsService, never())
        .updateRecord(mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId());

    // verify that second Run whose status could be updated has been updated
    verify(cromwellService).runStatus(runningRunEngineId2);
    verify(runsDao).updateRunStatus(runToUpdate2.runId(), COMPLETE);
    verify(wdsService)
        .updateRecord(mockRequest, runToUpdate2.runSet().recordType(), runToUpdate2.recordId());

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(runAlreadyCompleted.runId(), COMPLETE);

    assertEquals(3, actual.size());
    assertEquals(
        SYSTEM_ERROR,
        actual.stream().filter(r -> r.runId().equals(runningRunId1)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.runId().equals(runningRunId2)).toList().get(0).status());
    assertEquals(
        COMPLETE,
        actual.stream().filter(r -> r.runId().equals(completedRunId)).toList().get(0).status());
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

    when(cromwellService.runStatus(runningRunEngineId3))
        .thenReturn(new RunStatus().runId(runningRunEngineId3).state(State.EXECUTOR_ERROR));
    when(cromwellService.getRunErrors(runToUpdate3)).thenReturn(cromwellErrorMessage);
    when(runsDao.updateErrorMessage(runToUpdate3.runId(), cromwellErrorMessage)).thenReturn(1);
    when(runsDao.updateRunStatus(runToUpdate3.runId(), EXECUTOR_ERROR)).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate3));

    verify(cromwellService).runStatus(runningRunEngineId3);
    verify(runsDao).updateRunStatus(runToUpdate3.runId(), EXECUTOR_ERROR);
    verify(runsDao).updateErrorMessage(runToUpdate3.runId(), cromwellErrorMessage);

    assertEquals(
        EXECUTOR_ERROR,
        actual.stream().filter(r -> r.runId().equals(runningRunId3)).toList().get(0).status());

    assertEquals(
        "Workflow input processing failed (Required workflow input 'wf_hello.hello.addressee' not specified)",
        actual.stream()
            .filter(r -> r.runId().equals(runningRunId3))
            .toList()
            .get(0)
            .errorMessages());
  }

  @Test
  void hasOutputDefinitionReturnsTrue() throws JsonProcessingException {
    assertTrue(smartRunsPoller.hasOutputDefinition(runToUpdate1));
  }

  @Test
  void hasOutputDefinitionReturnsFalse() throws JsonProcessingException {
    String outputDefinition = "[]";
    RunSet outputLessRunSet =
        new RunSet(
            UUID.randomUUID(),
            new MethodVersion(
                UUID.randomUUID(),
                new Method(
                    UUID.randomUUID(),
                    "methodName",
                    "methodDescription",
                    methodCreatedTime,
                    UUID.randomUUID(),
                    "method source"),
                "version name",
                "version description",
                methodCreatedTime,
                UUID.randomUUID(),
                "file:///method/source/url"),
            "runSetName",
            "runSetDescription",
            false,
            CbasRunSetStatus.UNKNOWN,
            runSubmittedTime,
            runSubmittedTime,
            runSubmittedTime,
            0,
            0,
            "inputDefinition",
            outputDefinition,
            "entityType");

    Run run =
        new Run(
            runningRunId1,
            runningRunEngineId1,
            outputLessRunSet,
            runningRunEntityId1,
            runSubmittedTime,
            RUNNING,
            runningRunStatusUpdateTime,
            runningRunStatusUpdateTime,
            errorMessages);

    assertFalse(smartRunsPoller.hasOutputDefinition(run));
  }
}
