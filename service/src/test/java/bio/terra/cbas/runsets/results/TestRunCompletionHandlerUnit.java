package bio.terra.cbas.runsets.results;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsServiceException;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import cromwell.client.model.RunLog;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunCompletionHandler.class)
class TestRunCompletionHandlerUnit {

  private SmartRunsPoller smartRunsPoller;
  private RunDao runDao;

  static String outputs =
      """
        {
            "outputs": {
              "wf_hello.hello.salutations": "Hello batch!"
            }
        }
        """;

  static String emptyOutputs = """
        {
            "outputs":{}
        }
        """;

  @BeforeEach
  void init() {
    smartRunsPoller = mock(SmartRunsPoller.class);
    runDao = mock(RunDao.class);
  }

  @Test
  void updateRunResultsSucceededNoOutputsComplete() throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunResultsUpdateResult.SUCCESS, result);
  }

  @Test
  void updateRunResultsNoStatusChangeNoOutputsUpdateDateTime() throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(1);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunResultsUpdateResult.SUCCESS, result);
  }

  @Test
  void updateRunResultsNoStatusChangeNoOutputsUpdateDateTimeNoRecord()
      throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(0);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunResultsUpdateResult.ERROR, result);
  }

  @Test
  void updateRunResultsSucceededNoOutputsNoRecordsToUpdate() throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(0);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunResultsUpdateResult.ERROR, result);
  }

  @Test
  void updateRunResultsSucceededWithOutputsSaved()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputs, RunLog.class).getOutputs();

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);
    when(smartRunsPoller.hasOutputDefinition(run1Incomplete)).thenReturn(true);
    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunResultsUpdateResult.SUCCESS, result);
  }

  @Test
  void updateRunResultsSucceededWithEmptyOutputsSavedWarns()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);
    when(smartRunsPoller.hasOutputDefinition(run1Incomplete)).thenReturn(true);
    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs);
    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunResultsUpdateResult.SUCCESS, result);
  }

  @Test
  void updateRunResultsSucceededWithOutputsErrorProcessingSavedRecord()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputs, RunLog.class).getOutputs();

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);
    when(smartRunsPoller.hasOutputDefinition(run1Incomplete)).thenReturn(true);
    doThrow(new OutputProcessingException("Output processing error"))
        .when(smartRunsPoller)
        .updateOutputAttributes(run1Incomplete, cromwellOutputs);
    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);

    assertEquals(RunResultsUpdateResult.SUCCESS, result);
  }

  @Test
  void updateRunResultsSucceedErrorsPulledNull()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(SYSTEM_ERROR), any())).thenReturn(1);
    when(smartRunsPoller.getWorkflowErrors(run1Incomplete)).thenReturn(null);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(SYSTEM_ERROR), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunResultsUpdateResult.SUCCESS, result);
  }

  @Test
  void updateRunResultsSuccessErrorsPulled()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);
    ArrayList<String> errors = createResultGetWorkflowErrors();
    when(smartRunsPoller.getWorkflowErrors(any())).thenReturn(errors);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunResultsUpdateResult.SUCCESS, result);
  }

  @Test
  void updateRunResultsFailedErrorsPulledNoRecordUpdated()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(0);
    ArrayList<String> errors = createResultGetWorkflowErrors();
    when(smartRunsPoller.getWorkflowErrors(any())).thenReturn(errors);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunResultsUpdateResult.ERROR, result);
  }

  private Run createTestRun(UUID runId, CbasRunStatus status) {

    String engineId1 = "mockEngine1";
    String recordId1 = "mockRecordId1";
    OffsetDateTime submissionTimestamp1 = DateUtils.currentTimeInUTC();
    return new Run(
        runId, engineId1, null, recordId1, submissionTimestamp1, status, null, null, null);
  }

  private ArrayList<String> createResultGetWorkflowErrors() {
    ArrayList<String> errors = new ArrayList<>();
    errors.add("Workflow error1");
    errors.add("Workflow error2");
    return errors;
  }
}
