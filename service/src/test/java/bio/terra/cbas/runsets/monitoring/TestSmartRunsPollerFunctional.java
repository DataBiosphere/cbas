package bio.terra.cbas.runsets.monitoring;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.EXECUTOR_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.results.RunCompletionHandler;
import bio.terra.cbas.runsets.results.RunCompletionResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.gson.Gson;
import cromwell.client.ApiException;
import cromwell.client.model.FailureMessage;
import cromwell.client.model.RunLog;
import cromwell.client.model.WorkflowMetadataResponse;
import cromwell.client.model.WorkflowQueryResult;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  private SmartRunsPoller smartRunsPoller;
  private RunCompletionHandler runCompletionHandler;
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
          true,
          false,
          CbasRunSetStatus.UNKNOWN,
          runSubmittedTime,
          runSubmittedTime,
          runSubmittedTime,
          0,
          0,
          "inputDefinition",
          outputDefinition,
          "entityType",
          "user-foo");

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
    runCompletionHandler = mock(RunCompletionHandler.class);
    cbasApiConfiguration = mock(CbasApiConfiguration.class);
    when(cbasApiConfiguration.getMaxSmartPollRunUpdateSeconds()).thenReturn(1);
    smartRunsPoller =
        new SmartRunsPoller(cromwellService, runCompletionHandler, cbasApiConfiguration);
  }

  @Test
  void pollRunningRuns() throws Exception {
    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Running"));
    when(runCompletionHandler.updateResults(eq(runToUpdate1), any(), any(), any(), any()))
        .thenReturn(RunCompletionResult.SUCCESS);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(cromwellService, never()).runSummary(completedRunEngineId);
    verify(cromwellService, never()).getOutputs(runToUpdate1.engineId());
    verify(cromwellService, never()).getOutputs(runAlreadyCompleted.engineId());
    verify(cromwellService, never()).getRunErrors(runToUpdate1);
    verify(cromwellService, never()).getRunErrors(runAlreadyCompleted);

    verify(runCompletionHandler, never())
        .updateResults(eq(runAlreadyCompleted), any(), any(), any(), any());
    verify(runCompletionHandler, times(1))
        .updateResults(eq(runToUpdate1), any(), any(), any(), any());
    assertEquals(2, actual.updatedList().size());
  }

  @Test
  void updateNewlyCompletedRuns() throws Exception {
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

    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Succeeded"));
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    RunLog parseRunLog = object.fromJson(runLogValue, RunLog.class);
    Object workflowOutputs = parseRunLog.getOutputs();
    when(cromwellService.getOutputs(runningRunEngineId1)).thenReturn(workflowOutputs);

    when(runCompletionHandler.updateResults(
            eq(runToUpdate1),
            eq(COMPLETE),
            eq(workflowOutputs),
            eq(Collections.emptyList()),
            any()))
        .thenReturn(RunCompletionResult.SUCCESS);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(cromwellService).getOutputs(runningRunEngineId1);
    verify(runCompletionHandler, times(1))
        .updateResults(eq(runToUpdate1), eq(COMPLETE), any(), any(), any());

    // Make sure the already-completed workflow isn't re-updated:
    verify(runCompletionHandler, never())
        .updateResults(eq(runAlreadyCompleted), eq(COMPLETE), any(), any(), any());
    verify(cromwellService, never()).getOutputs(runAlreadyCompleted.engineId());

    assertEquals(2, actual.updatedList().size());
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
    when(runCompletionHandler.updateResults(eq(run1), any(), any(), any(), any()))
        .thenAnswer(
            i -> {
              pollOrder.add(run1.runId());
              return RunCompletionResult.SUCCESS;
            });
    when(runCompletionHandler.updateResults(eq(run2), any(), any(), any(), any()))
        .thenAnswer(
            i -> {
              pollOrder.add(run2.runId());
              return RunCompletionResult.SUCCESS;
            });
    when(runCompletionHandler.updateResults(eq(run3), any(), any(), any(), any()))
        .thenAnswer(
            i -> {
              pollOrder.add(run3.runId());
              return RunCompletionResult.SUCCESS;
            });

    // Note: the runs are out of last-polled-order here:
    var actual = smartRunsPoller.updateRuns(List.of(run3, run1, run2));
    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(cromwellService).runSummary(runningRunEngineId2);
    verify(cromwellService).runSummary(runningRunEngineId3);

    verify(cromwellService, never()).runSummary(completedRunEngineId);
    verify(runCompletionHandler, never())
        .updateResults(eq(runAlreadyCompleted), any(), any(), any(), any());
    verify(runCompletionHandler, times(1)).updateResults(eq(run1), any(), any(), any(), any());
    verify(runCompletionHandler, times(1)).updateResults(eq(run2), any(), any(), any(), any());
    verify(runCompletionHandler, times(1)).updateResults(eq(run3), any(), any(), any(), any());
    assertEquals(
        List.of(RUNNING, RUNNING, RUNNING),
        actual.updatedList().stream().map(Run::status).toList());

    assertEquals(pollOrder, List.of(run1.runId(), run2.runId(), run3.runId()));
  }

  @Test
  void haltPollingAfterTimeLimit() throws Exception {

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

    when(runCompletionHandler.updateResults(eq(run1), any(), any(), any(), any()))
        .thenAnswer(
            i -> {
              pollOrder.add(run1.runId());
              return 1;
            });
    when(runCompletionHandler.updateResults(eq(run2), any(), any(), any(), any()))
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
    verify(runCompletionHandler).updateResults(eq(run1), any(), any(), any(), any());
    verify(runCompletionHandler).updateResults(eq(run2), any(), any(), any(), any());
    verify(runCompletionHandler, never()).updateResults(eq(run3), any(), any(), any(), any());

    // Run 3 is in the result set, and keeps its previous status:
    assertEquals(
        List.of(RUNNING, RUNNING, RUNNING),
        actual.updatedList().stream().map(Run::status).toList());

    // Run 3 was never polled for:
    assertEquals(pollOrder, List.of(run1.runId(), run2.runId()));
  }

  @Test
  @SuppressWarnings("unchecked") // for List<String> captor
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
    ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);

    List<FailureMessage> listOfFails =
        objectMapper.readValue(cromwellError, WorkflowMetadataResponse.class).getFailures();
    String cromwellErrorMessage = CromwellService.getErrorMessage(listOfFails);

    when(cromwellService.runSummary(runningRunEngineId3))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId3).status("Failed"));
    when(cromwellService.getRunErrors(runToUpdate3)).thenReturn(cromwellErrorMessage);
    when(runCompletionHandler.updateResults(
            eq(runToUpdate3), eq(EXECUTOR_ERROR), any(), any(), any()))
        .thenReturn(RunCompletionResult.SUCCESS);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate3));

    verify(cromwellService).runSummary(runningRunEngineId3);
    verify(cromwellService).getRunErrors(runToUpdate3);
    verify(cromwellService, never()).getOutputs(any());
    verify(runCompletionHandler)
        .updateResults(eq(runToUpdate3), eq(EXECUTOR_ERROR), any(), captor.capture(), any());

    assertEquals(
        "Workflow input processing failed (Required workflow input 'wf_hello.hello.addressee' not specified)",
        captor.getValue().get(0));

    assertEquals(1, actual.updatedList().size());
  }

  @Test
  @SuppressWarnings("unchecked") // for List<String> captor
  void databaseUpdatedWhenGetCromwellErrorsThrows() throws Exception {
    ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);

    when(cromwellService.runSummary(runningRunEngineId3))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId3).status("Failed"));
    when(cromwellService.getRunErrors(runToUpdate3))
        .thenThrow(new ApiException("Cromwell client exception"));
    when(runCompletionHandler.updateResults(
            eq(runToUpdate3), eq(EXECUTOR_ERROR), any(), any(), any()))
        .thenReturn(RunCompletionResult.SUCCESS);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate3));

    verify(cromwellService).runSummary(runningRunEngineId3);
    verify(cromwellService).getRunErrors(runToUpdate3);
    verify(cromwellService, never()).getOutputs(any());
    verify(runCompletionHandler)
        .updateResults(eq(runToUpdate3), eq(EXECUTOR_ERROR), any(), captor.capture(), any());

    assertEquals(
        "Error fetching Cromwell-level error from Cromwell for workflow %s."
            .formatted(runningRunEngineId3),
        captor.getValue().get(0));

    assertEquals(
        "Error from Cromwell: %s"
            .formatted(new ApiException("Cromwell client exception").getMessage()),
        captor.getValue().get(1));

    assertEquals(1, actual.updatedList().size());
  }

  @Test
  @SuppressWarnings("unchecked") // for List<String> captor
  void databaseUpdatedWhenGetCromwellOutputsThrows() throws Exception {
    ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
    when(cromwellService.runSummary(runningRunEngineId1))
        .thenReturn(new WorkflowQueryResult().id(runningRunEngineId1).status("Succeeded"));
    when(cromwellService.getOutputs(runningRunEngineId1))
        .thenThrow(new ApiException("Cannot connect to Cromwell"));

    when(runCompletionHandler.updateResults(
            eq(runToUpdate1), eq(SYSTEM_ERROR), eq(null), eq(Collections.emptyList()), any()))
        .thenReturn(RunCompletionResult.SUCCESS);

    var actual = smartRunsPoller.updateRuns(List.of(runToUpdate1, runAlreadyCompleted));

    verify(cromwellService).runSummary(runningRunEngineId1);
    verify(runCompletionHandler, times(1))
        .updateResults(eq(runToUpdate1), eq(SYSTEM_ERROR), eq(null), captor.capture(), any());

    assertEquals(
        "Error while retrieving workflow outputs for record %s from run %s (engine workflow ID %s): %s"
            .formatted(
                runToUpdate1.recordId(),
                runToUpdate1.runId(),
                runToUpdate1.engineId(),
                "Cannot connect to Cromwell"),
        captor.getValue().get(0));

    assertEquals(2, actual.updatedList().size());
  }
}
