package bio.terra.cbas.controllers;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.common.exceptions.MissingRunOutputsException;
import bio.terra.cbas.common.exceptions.RunNotFoundException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.model.ErrorReport;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.model.RunResultsRequest;
import bio.terra.cbas.model.WorkflowTerminalState;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import bio.terra.cbas.runsets.results.RunCompletionHandler;
import bio.terra.cbas.runsets.results.RunCompletionResult;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest
@ContextConfiguration(classes = {RunsApiController.class, GlobalExceptionHandler.class})
class TestRunsApiController {

  private static final String API = "/api/batch/v1/runs";
  private static final String API_RESULTS = "/api/batch/v1/runs/results";

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private RunDao runDao;
  // The smart poller does most of the clever update logic, so we can test that separately. This
  // test just needs to make sure we call it properly and respect its updates
  @MockBean private SmartRunsPoller smartRunsPoller;
  @MockBean private RunCompletionHandler runsResultsManager;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  // The object mapper is pulled from the BeanConfig and used to convert to and from JSON in the
  // tests:
  @Autowired private ObjectMapper objectMapper;
  @MockBean private SamService samService;

  private static final UUID returnedRunId = UUID.randomUUID();
  private static final UUID returnedRunEngineId = UUID.randomUUID();
  private static final String returnedEntityId = UUID.randomUUID().toString();
  private static final OffsetDateTime methodCreatedTime = OffsetDateTime.now();
  private static final OffsetDateTime returnedSubmittedTime = OffsetDateTime.now();
  private static final OffsetDateTime runningStatusUpdateTime = OffsetDateTime.now();
  private static final OffsetDateTime completeStatusUpdateTime = OffsetDateTime.now();
  private static final String errorMessages = null;

  private static final UUID runSetId = UUID.randomUUID();
  private static final RunSet returnedRunSet =
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
              "methodurl"),
          "runSetName",
          "runSetDescription",
          true,
          false,
          CbasRunSetStatus.UNKNOWN,
          returnedSubmittedTime,
          returnedSubmittedTime,
          returnedSubmittedTime,
          0,
          0,
          "inputDefinition",
          "outputDefinition",
          "entitytype",
          "user-foo");

  private static final Run returnedRun =
      new Run(
          returnedRunId,
          returnedRunEngineId.toString(),
          returnedRunSet,
          returnedEntityId,
          returnedSubmittedTime,
          RUNNING,
          runningStatusUpdateTime,
          runningStatusUpdateTime,
          errorMessages);

  private static final Run updatedRun =
      new Run(
          returnedRunId,
          returnedRunEngineId.toString(),
          returnedRunSet,
          returnedEntityId,
          returnedSubmittedTime,
          COMPLETE,
          completeStatusUpdateTime,
          completeStatusUpdateTime,
          errorMessages);

  @Test
  void smartPollAndUpdateStatus() throws Exception {
    when(samService.hasReadPermission()).thenReturn(true);

    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));

    when(smartRunsPoller.updateRuns(eq(List.of(returnedRun))))
        .thenReturn(new UpdateResult<>(List.of(updatedRun), 1, 1, true));

    MvcResult result = mockMvc.perform(get(API)).andExpect(status().isOk()).andReturn();

    verify(smartRunsPoller).updateRuns(List.of(returnedRun));

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), RunLogResponse.class);

    assertEquals(1, parsedResponse.getRuns().size());
    RunLog runLog = parsedResponse.getRuns().get(0);

    assertEquals(returnedRunId, runLog.getRunId());
    assertEquals("methodurl", runLog.getWorkflowUrl());
    assertEquals("inputDefinition", runLog.getWorkflowParams());
    assertEquals("outputDefinition", runLog.getWorkflowOutputs());
    assertEquals(
        CbasRunStatus.toCbasApiState(COMPLETE), parsedResponse.getRuns().get(0).getState());
  }

  @Test
  void returnErrorForUserWithNoReadAccess() throws Exception {
    when(samService.hasReadPermission()).thenReturn(false);

    mockMvc
        .perform(get(API))
        .andExpect(status().isForbidden())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ForbiddenException))
        .andExpect(
            result ->
                assertEquals(
                    "User doesn't have 'read' permission on 'workspace' resource",
                    Objects.requireNonNull(result.getResolvedException()).getMessage()));
  }

  @Test
  void returnErrorForGetRequestWithoutToken() throws Exception {
    when(samService.hasReadPermission())
        .thenThrow(
            new BeanCreationException(
                "BearerToken bean instantiation failed.",
                new UnauthorizedException("Authorization header missing")));

    MvcResult response =
        mockMvc
            .perform(get(API))
            .andExpect(status().isUnauthorized())
            .andExpect(
                result ->
                    assertTrue(result.getResolvedException() instanceof BeanCreationException))
            .andReturn();

    // verify that the response object is of type ErrorReport and that the nested Unauthorized
    // exception was surfaced to user
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(401, errorResponse.getStatusCode());
    assertEquals("Authorization header missing", errorResponse.getMessage());
  }

  @Test
  void returnErrorForSamApiException() throws Exception {
    // throw a form of ErrorReportException which is thrown when an ApiException happens in
    // hasPermission()
    when(samService.hasReadPermission())
        .thenThrow(new SamUnauthorizedException("Exception thrown for testing purposes"));

    MvcResult response = mockMvc.perform(get(API)).andExpect(status().isUnauthorized()).andReturn();

    // verify that the response object is of type ErrorReport and that the exception message is set
    // properly
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(401, errorResponse.getStatusCode());
    assertEquals("Exception thrown for testing purposes", errorResponse.getMessage());
  }

  @Test
  void returnErrorForSamInterruptedException() throws Exception {
    // throw SamInterruptedException which is thrown when InterruptedException happens in
    // hasPermission()
    when(samService.hasReadPermission())
        .thenThrow(new SamInterruptedException("InterruptedException thrown for testing purposes"));

    MvcResult response =
        mockMvc.perform(get(API)).andExpect(status().isInternalServerError()).andReturn();

    // verify that the response object is of type ErrorReport and that the exception message is set
    // properly
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(500, errorResponse.getStatusCode());
    assertEquals("InterruptedException thrown for testing purposes", errorResponse.getMessage());
  }

  @Test
  void runResultsUpdateReturnsSuccessOnTerminalStatus() throws Exception {
    when(samService.hasWritePermission()).thenReturn(true);
    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));
    when(runsResultsManager.updateResults(any(), eq(COMPLETE), any(), any()))
        .thenReturn(RunCompletionResult.SUCCESS);

    var requestBody =
        new RunResultsRequest()
            .workflowId(returnedRunEngineId)
            .state(WorkflowTerminalState.SUCCEEDED)
            .outputs("{}");

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(0, result.getResponse().getContentLength());
  }

  @Test
  void runResultsUpdateReturnsSuccessOnFailedWithErrors() throws Exception {
    var errorList = List.of("error workflow engine", "system error");
    when(samService.hasWritePermission()).thenReturn(true);
    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));
    when(runsResultsManager.updateResults(
            eq(returnedRun), eq(CbasRunStatus.EXECUTOR_ERROR), eq(null), eq(errorList)))
        .thenReturn(RunCompletionResult.SUCCESS);

    var requestBody =
        new RunResultsRequest()
            .workflowId(UUID.fromString(updatedRun.engineId()))
            .state(WorkflowTerminalState.FAILED)
            .failures(errorList);

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(0, result.getResponse().getContentLength());
  }

  @Test
  void runResultsUpdateReturnsSystemErrorWhenUpdateThrows() throws Exception {
    when(samService.hasWritePermission()).thenReturn(true);
    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));
    when(runsResultsManager.updateResults(
            eq(returnedRun), eq(CbasRunStatus.SYSTEM_ERROR), eq(null), any()))
        .thenThrow(new RuntimeException("Failed to connect to database"));

    var requestBody =
        new RunResultsRequest().workflowId(returnedRunEngineId).state(WorkflowTerminalState.FAILED);

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andReturn();

    assertEquals(0, result.getResponse().getContentLength());
  }

  @Test
  void runResultsUpdateReturnsSystemErrorWhenUpdateErrors() throws Exception {
    when(samService.hasWritePermission()).thenReturn(true);
    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));
    when(runsResultsManager.updateResults(
            eq(returnedRun), eq(CbasRunStatus.SYSTEM_ERROR), eq(null), any()))
        .thenReturn(RunCompletionResult.ERROR);

    var requestBody =
        new RunResultsRequest()
            .workflowId(returnedRunEngineId)
            .state(WorkflowTerminalState.ABORTED);

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andReturn();

    assertEquals(0, result.getResponse().getContentLength());
  }

  @Test
  void runResultsUpdateReturnsUserErrorWhenValidationErrors() throws Exception {
    when(samService.hasWritePermission()).thenReturn(true);
    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));
    when(runsResultsManager.updateResults(
            eq(returnedRun), eq(CbasRunStatus.COMPLETE), any(), any()))
        .thenReturn(RunCompletionResult.VALIDATION);

    var requestBody =
        new RunResultsRequest()
            .workflowId(returnedRunEngineId)
            .state(WorkflowTerminalState.SUCCEEDED)
            .outputs("[]");

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(0, result.getResponse().getContentLength());
  }

  @Test
  void runResultsUpdateReturnsSuccessWhenUserHasNoPermission() throws Exception {
    when(samService.hasWritePermission()).thenReturn(false);
    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));
    when(runsResultsManager.updateResults(
            eq(returnedRun), eq(CbasRunStatus.SYSTEM_ERROR), any(), any()))
        .thenReturn(RunCompletionResult.SUCCESS);

    var requestBody =
        new RunResultsRequest()
            .workflowId(returnedRunEngineId)
            .state(WorkflowTerminalState.ABORTED);

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    verify(runsResultsManager)
        .updateResults(
            eq(returnedRun),
            eq(CbasRunStatus.SYSTEM_ERROR),
            ArgumentMatchers.isNull(),
            isNotNull());
    assertEquals(0, result.getResponse().getContentLength());
  }

  @Test
  void runResultsUpdateReturnsUserErrorWhenRunIdNotFound() throws Exception {
    when(samService.hasWritePermission()).thenReturn(true);
    when(runDao.getRuns(any())).thenReturn(Collections.emptyList());
    var requestBody =
        new RunResultsRequest()
            .workflowId(updatedRun.runId())
            .state(WorkflowTerminalState.SUCCEEDED)
            .outputs("{}");

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(r -> assertTrue(r.getResolvedException() instanceof RunNotFoundException))
            .andReturn();

    assertEquals(0, result.getResponse().getContentLength());
  }

  @Test
  void runResultsUpdateReturnsUserErrorWhenMissingOutputs() throws Exception {
    when(samService.hasWritePermission()).thenReturn(true);
    when(runDao.getRuns(any())).thenReturn(List.of(returnedRun));
    when(runsResultsManager.updateResults(eq(returnedRun), eq(COMPLETE), eq(null), any()))
        .thenReturn(RunCompletionResult.VALIDATION);

    var requestBody =
        new RunResultsRequest()
            .workflowId(updatedRun.runId())
            .state(WorkflowTerminalState.SUCCEEDED);

    MvcResult result =
        mockMvc
            .perform(
                post(API_RESULTS)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(
                r -> assertTrue(r.getResolvedException() instanceof MissingRunOutputsException))
            .andReturn();

    assertEquals(0, result.getResponse().getContentLength());
  }
}
