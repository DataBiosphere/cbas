package bio.terra.cbas.runsets.results;

import static bio.terra.cbas.models.CbasRunStatus.CANCELED;
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
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.cbas.dependencies.wds.WdsServiceException;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.MethodVersion;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.databiosphere.workspacedata.client.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunCompletionHandler.class)
class TestRunCompletionHandlerFunctional {

  private MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);

  private final String workspaceId = UUID.randomUUID().toString();

  public ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .setDateFormat(new StdDateFormat())
          .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);

  private RunDao runDao;
  private WdsService wdsService;
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

  static String outputs =
      """
        {
            "outputs": {
              "wf_hello.hello.salutation": "Hello batch!"
            }
        }
        """;

  static String outputsToThrow =
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
    runDao = mock(RunDao.class);
    wdsService = mock(WdsService.class);
  }

  @Test
  void updateRunCompletionSucceededNoOutputsComplete() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);

    UUID runId1 = UUID.randomUUID();
    RunSet runSet1 = createRunSet(UUID.randomUUID(), "[]");
    Run run1Incomplete = createTestRun(runId1, runSet1, RUNNING);

    // Set up mocks:
    when(runDao.getRuns(any())).thenReturn(List.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, null, Collections.emptyList());

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionWorkflowErrorsRecordedDateTime() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    var errorList = List.of("error1", "error 2");
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), any())).thenReturn(1);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, null, errorList);
    ArgumentCaptor<String> errors = ArgumentCaptor.forClass(String.class);
    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1)).updateRunStatusWithError(eq(runId1), any(), any(), errors.capture());
    verify(runDao, times(0)).updateLastPolledTimestamp(runId1);
    assertEquals("error1, error 2", errors.getValue());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionNoStatusChangeNoOutputsUpdateDateTimeNoRecord() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(0);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, null, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    assertEquals(RunCompletionResult.ERROR, result);
  }

  @Test
  void updateRunCompletionSucceededNoOutputsNoErrorsToUpdate() throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    UUID runId1 = UUID.randomUUID();
    RunSet runSet1 = createRunSet(UUID.randomUUID(), "[]");
    Run run1Incomplete = createTestRun(runId1, runSet1, RUNNING);

    // Set up mocks:
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.CANCELED), isA(OffsetDateTime.class)))
        .thenReturn(1);
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(
            run1Incomplete, CbasRunStatus.CANCELED, null, Collections.emptyList());

    // Validate the results:
    verify(wdsService, times(0)).updateRecord(any(), any(), any());
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(CANCELED), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSucceededNoStatusUpdate() throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    UUID runId1 = UUID.randomUUID();
    RunSet runSet1 = createRunSet(UUID.randomUUID(), outputDefinition);
    Run run1Incomplete = createTestRun(runId1, runSet1, CANCELED);

    // Set up mocks:
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(1);
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(
            run1Incomplete, CbasRunStatus.CANCELED, "[]", Collections.emptyList());

    // Validate the results:
    verify(wdsService, times(0)).updateRecord(any(), any(), any());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionFailedNoRecordsUpdated() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    UUID runId1 = UUID.randomUUID();
    RunSet runSet1 = createRunSet(UUID.randomUUID(), "[]");
    Run run1Incomplete = createTestRun(runId1, runSet1, RUNNING);

    // Set up mocks:
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.CANCELED), isA(OffsetDateTime.class)))
        .thenReturn(0);
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(
            run1Incomplete, CbasRunStatus.CANCELED, null, Collections.emptyList());

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(CANCELED), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    assertEquals(RunCompletionResult.ERROR, result);
    assertEquals(RUNNING, run1Incomplete.status());
  }

  @Test
  void updateRunCompletionSucceededWithOutputsSaved() throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    // Set up run to expect non-empty outputs
    RunSet runSet1 = createRunSet(UUID.randomUUID(), outputDefinition);
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, runSet1, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputs, RunLog.class).getOutputs();

    // Set up mocks:
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(wdsService, times(1)).updateRecord(any(), any(), any());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSucceededWithEmptyOutputs() throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    // Set up run to expect non-empty outputs
    RunSet runSet = createRunSet(UUID.randomUUID(), "[]");
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, runSet, RUNNING);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    // Set up mocks:
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, null);
    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(wdsService, times(0)).updateRecord(any(), any(), any());

    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSucceededWhenWdsThrowsSavedStatusWithErrors() throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    // Set up run to expect non-empty outputs
    RunSet runSet = createRunSet(UUID.randomUUID(), outputDefinition);
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, runSet, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputs, RunLog.class).getOutputs();
    List<String> failures = createWorkflowErrorsList();

    // Set up mocks:
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);
    // WDS throws
    doThrow(new WdsServiceApiException(new ApiException("Some API error")) {})
        .when(wdsService)
        .updateRecord(any(), any(), any());
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, failures);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());

    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionReturnsValidationWithOutputsErrorProcessing() throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);
    // Set up run to expect non-empty outputs
    RunSet runSet = createRunSet(UUID.randomUUID(), outputDefinition);
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, runSet, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputsToThrow, RunLog.class).getOutputs();
    List<String> failures = createWorkflowErrorsList();

    // Set up mocks:
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, failures);

    // Validate the results:
    verify(wdsService, times(0)).updateRecord(any(), any(), any());
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());

    assertEquals(RunCompletionResult.VALIDATION_ERROR, result);
  }

  @Test
  void updateRunCompletionSucceededWithEmptyOutputsNoFailures() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.updateRunStatus(eq(runId1), eq(SYSTEM_ERROR), any())).thenReturn(1);

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs, null);

    // Validate the results:
    // Since error count is 0, only Status is going to be updated in DB.
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(SYSTEM_ERROR), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionSuccessSavingFailures() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);
    List<String> errors = createWorkflowErrorsList();

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs, errors);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionFailedErrorsPulledNoRecordUpdated() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(0);
    List<String> errors = createWorkflowErrorsList();

    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, cromwellOutputs, errors);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    assertEquals(RunCompletionResult.ERROR, result);
  }

  @Test
  void updateRunCompletionSuccessWithEmptyFailures() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper, micrometerMetrics);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
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
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  private Run createTestRun(UUID runId, RunSet runSet, CbasRunStatus status) {

    String engineId1 = "mockEngine1";
    String recordId1 = "mockRecordId1";
    OffsetDateTime submissionTimestamp1 = DateUtils.currentTimeInUTC();
    return new Run(
        runId,
        engineId1,
        runSet,
        recordId1,
        submissionTimestamp1,
        status,
        null,
        null,
        null,
        workspaceId);
  }

  private RunSet createRunSet(UUID runSetId, String outputDefinition) {
    return new RunSet(
        runSetId,
        new MethodVersion(
            UUID.randomUUID(),
            null,
            "version name",
            "version description",
            OffsetDateTime.now(),
            runSetId,
            "file:///method/source/url",
            workspaceId),
        "runSetName",
        "runSetDescription",
        true,
        false,
        CbasRunSetStatus.UNKNOWN,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        0,
        0,
        "inputDefinition",
        outputDefinition,
        "entityType",
        "user-foo",
        workspaceId);
  }

  private List<String> createWorkflowErrorsList() {
    return List.of("Workflow error1", "Workflow error2");
  }
}
