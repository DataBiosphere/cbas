package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.EXECUTOR_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
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
import cromwell.client.model.WorkflowMetadataResponse;
import cromwell.client.model.WorkflowQueryResult;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SmartRunsPoller.class)
public class TestSmartRunsPollerFunctional {

  private static final OffsetDateTime methodCreatedTime = OffsetDateTime.now();
  private static final OffsetDateTime runSubmittedTime = OffsetDateTime.now();
  private static final UUID runningRunId1 = UUID.randomUUID();
  private static final String runningRunEngineId1 = UUID.randomUUID().toString();
  private static final String runningRunEntityId1 = UUID.randomUUID().toString();
  // Set the last status update a few minutes ago to avoid tripping over the "recent poll
  // ineligibility" requirement:
  private static final OffsetDateTime runningRunStatusUpdateTime =
      OffsetDateTime.now().minusMinutes(5);
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
  private CbasApiConfiguration cbasApiConfiguration;

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
    cbasApiConfiguration = mock(CbasApiConfiguration.class);
    when(cbasApiConfiguration.getMaxSmartPollRunUpdateSeconds()).thenReturn(1);
    smartRunsPoller =
        new SmartRunsPoller(
            cromwellService, runsDao, wdsService, objectMapper, cbasApiConfiguration);
  }

  @Test
  void pollRunningRuns() throws Exception {
    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Running"));

    when(runsDao.updateLastPolledTimestamp(runToUpdate1.runId())).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(runsDao).updateLastPolledTimestamp(runToUpdate1.runId());
    verify(cromwellService, never()).runSummary(completedRunEngineId);
    verify(runsDao, never()).updateLastPolledTimestamp(runAlreadyCompleted.runId());

    assertEquals(2, actual.updatedList().size());
    assertEquals(
        RUNNING,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(runningRunId1))
            .toList()
            .get(0)
            .status());
    assertEquals(
        COMPLETE,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(completedRunId))
            .toList()
            .get(0)
            .status());
  }

  @Test
  void updateNewlyCompletedRuns() throws Exception {
    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Succeeded"));

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

    when(runsDao.updateRunStatus(eq(runToUpdate1.runId()), eq(COMPLETE), any())).thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService)
        .runSummary(runningRunEngineId1); // (verify that the workflow is in a failed state)
    verify(runsDao)
        .updateRunStatus(
            eq(runToUpdate1.runId()),
            eq(COMPLETE),
            any()); // (verify that error messages are received from Cromwell)
    verify(wdsService)
        .updateRecord(mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId());

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(eq(runAlreadyCompleted.runId()), eq(COMPLETE), any());

    assertEquals(2, actual.updatedList().size());
    assertEquals(
        COMPLETE,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(runningRunId1))
            .toList()
            .get(0)
            .status());
    assertEquals(
        COMPLETE,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(completedRunId))
            .toList()
            .get(0)
            .status());
  }

  @Test
  void updatingOutputFails_UpdatingDataTable() throws Exception {
    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Succeeded"));

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
    when(wdsService.updateRecord(
            mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId()))
        .thenThrow(
            new WdsServiceApiException(
                new org.databiosphere.workspacedata.client.ApiException("Bad WDS update")));
    when(runsDao.updateRunStatusWithError(eq(runningRunId1), eq(SYSTEM_ERROR), any(), any()))
        .thenReturn(1);
    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService).runSummary(runningRunEngineId1);
    String expectedErrorMessage =
        "Error while updating data table attributes for record %s from run %s (engine workflow ID %s): WdsServiceApiException: Bad WDS update"
            .formatted(runToUpdate1.recordId(), runToUpdate1.runId(), runToUpdate1.engineId());
    verify(runsDao)
        .updateRunStatusWithError(
            eq(runToUpdate1.runId()),
            eq(SYSTEM_ERROR),
            any(),
            eq(expectedErrorMessage)); // (verify that error messages are recorded)
    verify(wdsService)
        .updateRecord(mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId());

    assertEquals(2, actual.updatedList().size());
    assertEquals(
        SYSTEM_ERROR,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(runningRunId1))
            .toList()
            .get(0)
            .status());
    assertEquals(
        expectedErrorMessage,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(runningRunId1))
            .toList()
            .get(0)
            .errorMessages());
  }

  @Test
  void pollRunsInLeastRecentlyPolledOrder() throws Exception {
    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Running"));
    when(cromwellService.runSummary(runningRunEngineId2))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId2).status("Running"));
    when(cromwellService.runSummary(runningRunEngineId3))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId3).status("Running"));

    var run1 = runToUpdate1.withLastPolled(OffsetDateTime.now().minusSeconds(1000));
    var run2 = runToUpdate2.withLastPolled(OffsetDateTime.now().minusSeconds(500));
    var run3 =
        runToUpdate3.withLastPolled(OffsetDateTime.now().minusSeconds(100)).withStatus(RUNNING);

    ArrayList<UUID> pollOrder = new ArrayList<>();

    when(runsDao.updateLastPolledTimestamp(run1.runId()))
        .thenAnswer(
            i -> {
              pollOrder.add(run1.runId());
              return 1;
            });
    when(runsDao.updateLastPolledTimestamp(run2.runId()))
        .thenAnswer(
            i -> {
              pollOrder.add(run2.runId());
              return 1;
            });
    when(runsDao.updateLastPolledTimestamp(run3.runId()))
        .thenAnswer(
            i -> {
              pollOrder.add(run3.runId());
              return 1;
            });

    // Note: the runs are out of last-polled-order here:
    var actual = smartRunsPoller.updateRuns(List.of(run3, run1, run2));

    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(cromwellService).runSummary(runningRunEngineId2);
    verify(cromwellService).runSummary(runningRunEngineId3);
    verify(runsDao).updateLastPolledTimestamp(runToUpdate1.runId());
    verify(cromwellService, never()).runSummary(completedRunEngineId);
    verify(runsDao, never()).updateLastPolledTimestamp(runAlreadyCompleted.runId());

    assertEquals(
        List.of(RUNNING, RUNNING, RUNNING),
        actual.updatedList().stream().map(Run::status).toList());

    assertEquals(pollOrder, List.of(run1.runId(), run2.runId(), run3.runId()));
  }

  @Test
  void haltPollingAfterTimeLimit() throws Exception {

    OffsetDateTime testStart = OffsetDateTime.now();

    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Running"));
    when(cromwellService.runSummary(runningRunEngineId2))
        .thenAnswer(
            i -> {
              Thread.sleep(1000); // The timeout is configured (via mock) to be 1 second
              return new WorkflowQueryResult().id(runningRunEngineId2).status("Running");
            });
    when(cromwellService.runSummary(runningRunEngineId3))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId3).status("Running"));

    var run1 = runToUpdate1.withLastPolled(OffsetDateTime.now().minusSeconds(1000));
    var run2 = runToUpdate2.withLastPolled(OffsetDateTime.now().minusSeconds(500));
    var run3 =
        runToUpdate3.withLastPolled(OffsetDateTime.now().minusSeconds(100)).withStatus(RUNNING);

    ArrayList<UUID> pollOrder = new ArrayList<>();

    when(runsDao.updateLastPolledTimestamp(run1.runId()))
        .thenAnswer(
            i -> {
              pollOrder.add(run1.runId());
              return 1;
            });
    when(runsDao.updateLastPolledTimestamp(run2.runId()))
        .thenAnswer(
            i -> {
              pollOrder.add(run2.runId());
              return 1;
            });

    // Note: the runs are out of last-polled-order here:
    var actual = smartRunsPoller.updateRuns(List.of(run3, run1, run2));

    // We poll for the first two summaries, but not the third:
    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(cromwellService).runSummary(runningRunEngineId2);
    verify(cromwellService, never()).runSummary(runningRunEngineId3);
    // We update the first two polled timestamps, but not the third:
    verify(runsDao).updateLastPolledTimestamp(run1.runId());
    verify(runsDao).updateLastPolledTimestamp(run2.runId());
    verify(runsDao, never()).updateLastPolledTimestamp(run3.runId());

    // Run 3 is in the result set, and keeps its previous status:
    assertEquals(
        List.of(RUNNING, RUNNING, RUNNING),
        actual.updatedList().stream().map(Run::status).toList());

    // Run 3 was never polled for:
    assertEquals(pollOrder, List.of(run1.runId(), run2.runId()));
  }

  @Test
  void updatingOutputFails_InputMissing() throws Exception {
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
    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Succeeded"));
    when(cromwellService.runSummary(runningRunEngineId2))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId2).status("Succeeded"));
    when(runsDao.updateRunStatusWithError(eq(runToUpdate1.runId()), eq(SYSTEM_ERROR), any(), any()))
        .thenReturn(1);
    when(runsDao.updateRunStatus(eq(runToUpdate2.runId()), eq(COMPLETE), any())).thenReturn(1);

    var actual =
        smartRunsPoller.updateRuns(List.of(runToUpdate1, runToUpdate2, runAlreadyCompleted));

    // verify that Run is marked as Failed as attaching outputs failed
    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(runsDao)
        .updateRunStatusWithError(
            eq(runToUpdate1.runId()),
            eq(SYSTEM_ERROR),
            any(),
            eq(
                "Error while updating data table attributes for record %s from run %s (engine workflow ID %s): Output wf_hello.hello.salutation not found in workflow outputs."
                    .formatted(
                        runToUpdate1.recordId(), runToUpdate1.runId(), runToUpdate1.engineId())));
    verify(wdsService, never())
        .updateRecord(mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId());

    // verify that second Run whose status could be updated has been updated
    verify(cromwellService).runSummary(runningRunEngineId2);
    verify(runsDao).updateRunStatus(eq(runToUpdate2.runId()), eq(COMPLETE), any());
    verify(wdsService)
        .updateRecord(mockRequest, runToUpdate2.runSet().recordType(), runToUpdate2.recordId());

    // Make sure the already-completed workflow isn't re-updated:
    verify(runsDao, never()).updateRunStatus(eq(runAlreadyCompleted.runId()), eq(COMPLETE), any());

    assertEquals(3, actual.updatedList().size());
    assertEquals(
        SYSTEM_ERROR,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(runningRunId1))
            .toList()
            .get(0)
            .status());
    assertEquals(
        COMPLETE,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(runningRunId2))
            .toList()
            .get(0)
            .status());
    assertEquals(
        COMPLETE,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(completedRunId))
            .toList()
            .get(0)
            .status());
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

    when(cromwellService.runSummary(runningRunEngineId3))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId3).status("Failed"));
    when(cromwellService.getRunErrors(runToUpdate3)).thenReturn(cromwellErrorMessage);
    when(runsDao.updateRunStatusWithError(
            eq(runToUpdate3.runId()), eq(EXECUTOR_ERROR), any(), any()))
        .thenReturn(1);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate3));

    verify(cromwellService).runSummary(runningRunEngineId3);
    verify(runsDao)
        .updateRunStatusWithError(
            eq(runToUpdate3.runId()), eq(EXECUTOR_ERROR), any(), eq(cromwellErrorMessage));

    assertEquals(
        EXECUTOR_ERROR,
        actual.updatedList().stream()
            .filter(r -> r.runId().equals(runningRunId3))
            .toList()
            .get(0)
            .status());

    assertEquals(
        "Workflow input processing failed (Required workflow input 'wf_hello.hello.addressee' not specified)",
        actual.updatedList().stream()
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
