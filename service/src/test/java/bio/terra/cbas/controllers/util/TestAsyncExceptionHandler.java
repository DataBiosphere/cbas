package bio.terra.cbas.controllers.util;

import static bio.terra.cbas.models.CbasRunStatus.QUEUED;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.WdsRecordSet;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestAsyncExceptionHandler {

  private final RunDao runDao = mock(RunDao.class);
  private final RunSetDao runSetDao = mock(RunSetDao.class);

  private final AsyncExceptionHandler mockAsyncExceptionHandler =
      new AsyncExceptionHandler(runDao, runSetDao);

  private final UUID runSetId = UUID.randomUUID();
  private final UUID runId1 = UUID.randomUUID();
  private final UUID runId2 = UUID.randomUUID();
  private final UUID runId3 = UUID.randomUUID();
  private final UUID workspaceId = UUID.randomUUID();

  private final RunSet runSet =
      new RunSet(
          runSetId,
          null,
          "",
          "",
          false,
          false,
          CbasRunSetStatus.QUEUED,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          3,
          0,
          "inputdefinition",
          "outputDefinition",
          "FOO",
          "user-subject-id",
          workspaceId);

  private final RunSetRequest runSetRequest =
      new RunSetRequest().wdsRecords(new WdsRecordSet().recordIds(List.of("FOO1", "FOO2", "FOO3")));

  private final Throwable throwable = new Throwable("exception thrown for testing");
  String expectedErrorMsg =
      "Something went wrong while submitting workflows. Error: exception thrown for testing";

  private final Object[] params = {runSetRequest, runSet};

  private Run createQueuedRun(UUID runId) {
    return new Run(
        runId, UUID.randomUUID().toString(), runSet, null, null, QUEUED, null, null, null);
  }

  @Test
  // test case for Run Set with 3 Runs all in Queued state
  void handleErrorForAllRunsInQueuedState() {
    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, List.of(QUEUED))))
        .thenReturn(
            List.of(createQueuedRun(runId1), createQueuedRun(runId2), createQueuedRun(runId3)));

    mockAsyncExceptionHandler.handleExceptionFromAsyncSubmission(
        throwable, "triggerWorkflowSubmission", params);

    // verify all 3 Runs were marked in Error state
    verify(runDao)
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), eq(expectedErrorMsg));
    verify(runDao)
        .updateRunStatusWithError(eq(runId2), eq(SYSTEM_ERROR), any(), eq(expectedErrorMsg));
    verify(runDao)
        .updateRunStatusWithError(eq(runId3), eq(SYSTEM_ERROR), any(), eq(expectedErrorMsg));

    // verify that RunSet was also marked in Error state
    verify(runSetDao)
        .updateStateAndRunSetDetails(eq(runSetId), eq(CbasRunSetStatus.ERROR), eq(3), eq(3), any());
  }

  @Test
  // test case for Run Set with 3 Runs all in non-Queued state
  void noErrorHandlingForSubmittedRuns() {
    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, List.of(QUEUED)))).thenReturn(List.of());

    mockAsyncExceptionHandler.handleExceptionFromAsyncSubmission(
        throwable, "triggerWorkflowSubmission", params);

    // verify that Runs weren't marked in Error state as they were already submitted
    verify(runDao, never()).updateRunStatusWithError(any(), any(), any(), any());

    // verify that Run Set wasn't marked in Error state
    verify(runSetDao, never()).updateStateAndRunSetDetails(any(), any(), any(), any(), any());
  }

  @Test
  // test case for Run Set with 1 Run in Queued state and 2 in non-Queued state
  void handleErrorForOneRunInQueuedState() {
    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, List.of(QUEUED))))
        .thenReturn(List.of(createQueuedRun(runId3)));

    mockAsyncExceptionHandler.handleExceptionFromAsyncSubmission(
        throwable, "triggerWorkflowSubmission", params);

    // verify Run 1 and 2 weren't marked in Error state
    verify(runDao, never())
        .updateRunStatusWithError(eq(runId1), eq(SYSTEM_ERROR), any(), eq(expectedErrorMsg));
    verify(runDao, never())
        .updateRunStatusWithError(eq(runId2), eq(SYSTEM_ERROR), any(), eq(expectedErrorMsg));

    // verify Run 3 was marked in Error state
    verify(runDao)
        .updateRunStatusWithError(eq(runId3), eq(SYSTEM_ERROR), any(), eq(expectedErrorMsg));

    // verify that RunSet wasn't in Error state
    verify(runSetDao, never()).updateStateAndRunSetDetails(any(), any(), any(), any(), any());
  }
}
