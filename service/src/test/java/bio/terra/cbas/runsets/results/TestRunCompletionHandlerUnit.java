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
import java.util.Collections;
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
  void updateRunCompletionSucceededNoOutputsComplete() throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, null, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionNoStatusChangeNoOutputsUpdateDateTime() throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(1);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, null, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionNoStatusChangeNoOutputsUpdateDateTimeNoRecord()
      throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(0);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, null, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunCompletionResult.ERROR, result);
  }

  @Test
  void updateRunCompletionSucceededNoOutputsNoRecordsToUpdate() throws JsonProcessingException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(0);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, null, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(RunCompletionResult.ERROR, result);
  }

  @Test
  void updateRunCompletionSucceededWithOutputsSaved()
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
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSucceededWithEmptyOutputsSavedWarns()
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
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, null);
    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSucceededWithOutputsErrorProcessingSavedRecord()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunCompletionHandler runCompletionHandler = new RunCompletionHandler(runDao, smartRunsPoller);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputs, RunLog.class).getOutputs();
    List<String> failures = List.of("workflow error 1", "workflow error 2");

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);
    when(smartRunsPoller.hasOutputDefinition(run1Incomplete)).thenReturn(true);
    doThrow(new OutputProcessingException("Output processing error"))
        .when(smartRunsPoller)
        .updateOutputAttributes(run1Incomplete, cromwellOutputs);
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);

    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSucceedFailuresNull()
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

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs, null);

    // Validate the results:
    // Since error count is 0, only Status is going to be updated in DB.
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(SYSTEM_ERROR), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSuccessSavingFailures()
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
    List<String> errors = createResultGetWorkflowErrors();

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs, errors);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionFailedErrorsPulledNoRecordUpdated()
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
    List<String> errors = createResultGetWorkflowErrors();

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs, errors);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunCompletionResult.ERROR, result);
  }

  @Test
  void updateRunCompletionSuccessWithEmptyFailures()
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
    List<String> failures = Collections.emptyList();

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs, failures);

    // Validate the results:
    // Since error count is 0, only Status is going to be updated in DB.
    verify(runDao, times(1)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  private Run createTestRun(UUID runId, CbasRunStatus status) {

    String engineId1 = "mockEngine1";
    String recordId1 = "mockRecordId1";
    OffsetDateTime submissionTimestamp1 = DateUtils.currentTimeInUTC();
    return new Run(
        runId, engineId1, null, recordId1, submissionTimestamp1, status, null, null, null);
  }

  private List<String> createResultGetWorkflowErrors() {
    List<String> errors = List.of("Workflow error1", "Workflow error2");
    return errors;
  }
}
