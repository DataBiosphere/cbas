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
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunCompletionHandler.class)
class TestRunCompletionHandlerUnit {

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
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, null, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionNoStatusChangeNoOutputsUpdateDateTime() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
    when(runDao.updateLastPolledTimestamp(runId1)).thenReturn(1);

    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, SYSTEM_ERROR, null, null);

    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), any(), any(), anyString());
    verify(runDao, times(1)).updateLastPolledTimestamp(runId1);
    assertEquals(RunCompletionResult.SUCCESS, result);
  }

  @Test
  void updateRunCompletionNoStatusChangeNoOutputsUpdateDateTimeNoRecord() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, SYSTEM_ERROR);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
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
  void updateRunCompletionSucceededNoOutputsNoRecordsToUpdate() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(0);
    // Run the results update:
    var result = runCompletionHandler.updateResults(run1Incomplete, COMPLETE, null, null);

    // Validate the results:
    verify(runDao, times(1)).updateRunStatus(eq(runId1), eq(COMPLETE), any());
    verify(runDao, times(0)).updateRunStatusWithError(eq(runId1), eq(COMPLETE), any(), anyString());
    assertEquals(RunCompletionResult.ERROR, result);
  }

  @Test
  void updateRunCompletionSucceededWithOutputsSaved() throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper);
    // Set up run to expect non-empty outputs
    RunSet runSet = createRunSet(UUID.randomUUID());
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, runSet, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputs, RunLog.class).getOutputs();

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
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
  void updateRunCompletionSucceededWithUnexpectedEmptyOutputsSavedWithError()
      throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper);
    // Set up run to expect non-empty outputs
    RunSet runSet = createRunSet(UUID.randomUUID());
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, runSet, RUNNING);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
    when(runDao.updateRunStatus(eq(runId1), eq(CbasRunStatus.COMPLETE), isA(OffsetDateTime.class)))
        .thenReturn(1);
    // Run the results update:
    var result =
        runCompletionHandler.updateResults(run1Incomplete, COMPLETE, cromwellOutputs, null);
    // Validate the results:
    verify(runDao, times(0)).updateRunStatus(eq(runId1), any(), any());
    verify(runDao, times(1))
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString());
    verify(wdsService, times(0)).updateRecord(any(), any(), any());

    assertEquals(RunCompletionResult.ERROR, result);
  }

  @Test
  void updateRunCompletionSucceededWithOutputsErrorProcessingSavedRecord()
      throws WdsServiceException {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper);
    // Set up run to expect non-empty outputs
    RunSet runSet = createRunSet(UUID.randomUUID());
    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, runSet, RUNNING);
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(outputs, RunLog.class).getOutputs();
    List<String> failures = createWorkflowErrorsList();

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
    when(runDao.updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), anyString()))
        .thenReturn(1);
    doThrow(new RuntimeException("Some WDS error"))
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
  void updateRunCompletionSucceedFailuresNull() {
    RunCompletionHandler runCompletionHandler =
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
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
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
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
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
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
        new RunCompletionHandler(runDao, wdsService, objectMapper);

    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    Object cromwellOutputs = object.fromJson(emptyOutputs, RunLog.class).getOutputs();

    UUID runId1 = UUID.randomUUID();
    Run run1Incomplete = createTestRun(runId1, null, RUNNING);

    // Set up mocks:
    when(runDao.getRunByIdIfExists(any())).thenReturn(Optional.of(run1Incomplete));
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
        runId, engineId1, runSet, recordId1, submissionTimestamp1, status, null, null, null);
  }

  private RunSet createRunSet(UUID runSetId) {
    return new RunSet(
        runSetId,
        new MethodVersion(
            UUID.randomUUID(),
            null,
            "version name",
            "version description",
            OffsetDateTime.now(),
            runSetId,
            "file:///method/source/url"),
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
        "user-foo");
  }

  private List<String> createWorkflowErrorsList() {
    return List.of("Workflow error1", "Workflow error2");
  }
}
