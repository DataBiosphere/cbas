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
import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsServiceException;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import cromwell.client.ApiException;
import cromwell.client.model.RunLog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunResultsManager.class)
class TestRunResultsManagerUnit {

  private SmartRunsPoller smartRunsPoller;
  private CromwellService cromwellService;
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
    cromwellService = mock(CromwellService.class);
  }

  @Test
  void updateRunResultsSucceededNoOutputsComplete() throws JsonProcessingException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);

    // Run the results update:
    var result = runResultsManager.updateResults(run1Incomplete, COMPLETE, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(new RunResultsUpdateResponse(true, null), result);
  }

  @Test
  void updateRunResultsNoStatusChangeNoOutputsUpdateDateTime() throws JsonProcessingException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, COMPLETE);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(1);

    // Run the results update:
    var result = runResultsManager.updateResults(run1Incomplete, COMPLETE, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(new RunResultsUpdateResponse(true, null), result);
  }

  @Test
  void updateRunResultsNoStatusChangeNoOutputsUpdateDateTimeNoRecord()
      throws JsonProcessingException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, COMPLETE);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(0);

    // Run the results update:
    var result = runResultsManager.updateResults(run1Incomplete, COMPLETE, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(new RunResultsUpdateResponse(true, null), result);
  }

  @Test
  void updateRunResultsSucceededNoOutputsNoRecordsToUpdate() throws JsonProcessingException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(0);

    // Run the results update:
    var result = runResultsManager.updateResults(run1Incomplete, COMPLETE, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    verify(smartRunsPoller, times(0)).hasOutputDefinition(run1Incomplete);
    assertEquals(
        new RunResultsUpdateResponse(
            false,
            "Run %s was attempted to update status from RUNNING to COMPLETE but no DB rows were changed by the query."
                .formatted(runId1)),
        result);
  }

  @Test
  void updateRunResultsSucceededWithOutputsSaved()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          DependencyNotAvailableException, CoercionException, AzureAccessTokenException,
          ApiException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

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
    var result = runResultsManager.updateResults(run1Incomplete, COMPLETE, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(new RunResultsUpdateResponse(true, null), result);
  }

  @Test
  void updateRunResultsSucceededWithEmptyOutputsSavedWarns()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

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
    var result = runResultsManager.updateResults(run1Incomplete, COMPLETE, cromwellOutputs);
    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(new RunResultsUpdateResponse(true, null), result);
  }

  @Test
  void updateRunResultsSucceededWithOutputsErrorProcessingSavedRecord()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

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
    var result = runResultsManager.updateResults(run1Incomplete, COMPLETE, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(1)).updateOutputAttributes(run1Incomplete, cromwellOutputs);

    assertEquals(new RunResultsUpdateResponse(true, null), result);
  }

  @Test
  void updateRunResultsFailedErrorsPulled()
      throws JsonProcessingException, WdsServiceException, OutputProcessingException,
          CoercionException, ApiException {
    RunResultsManager runResultsManager =
        new RunResultsManager(runDao, smartRunsPoller, cromwellService);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);
    when(cromwellService.getRunErrors(run1Incomplete)).thenReturn("Run execution error");

    // Run the results update:
    var result = runResultsManager.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(smartRunsPoller, times(0)).updateOutputAttributes(run1Incomplete, cromwellOutputs);
    assertEquals(new RunResultsUpdateResponse(true, null), result);
  }

  private Run createTestRun(UUID runId, CbasRunStatus status) {

    String engineId1 = "mockEngine1";
    String recordId1 = "mockRecordId1";
    OffsetDateTime submissionTimestamp1 = DateUtils.currentTimeInUTC();
    return new Run(
        runId, engineId1, null, recordId1, submissionTimestamp1, status, null, null, null);
  }
}
