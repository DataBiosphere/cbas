package bio.terra.cbas.runsets.results;

import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
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
import cromwell.client.model.RunLog;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunCompletionHandler.class)
public class TestRunCompletionHandlerFunctional {

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
  private static final String errorMessages = null;

  public ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .setDateFormat(new StdDateFormat())
          .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
  private RunDao runsDao;
  private WdsService wdsService;
  private RunCompletionHandler runCompletionHandler;

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

  @BeforeEach
  public void init() {
    runsDao = mock(RunDao.class);
    wdsService = mock(WdsService.class);
    runCompletionHandler = new RunCompletionHandler(runsDao, wdsService, objectMapper);
  }

  @Test
  void updatingOutputFails_UpdatingDataTable() throws Exception {
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
    when(wdsService.updateRecord(
            mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId()))
        .thenThrow(
            new WdsServiceApiException(
                new org.databiosphere.workspacedata.client.ApiException("Bad WDS update")));
    Assertions.assertThrows(
        WdsServiceApiException.class,
        () -> runCompletionHandler.updateOutputAttributes(runToUpdate1, parseRunLog.getOutputs()));

    // verify that results update failed as attaching outputs failed
    verify(wdsService)
        .updateRecord(mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId());
    verify(runsDao, never()).updateRunStatusWithError(any(), any(), any(), any());
    verify(runsDao, never()).updateRunStatus(any(), any(), any());
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

    RecordAttributes mockAttributes = new RecordAttributes();
    mockAttributes.put("foo_name", "Hello batch!");
    RecordRequest mockRequest = new RecordRequest().attributes(mockAttributes);

    Assertions.assertThrows(
        OutputProcessingException.class,
        () -> runCompletionHandler.updateOutputAttributes(runToUpdate2, misspelledCromwellOutputs));

    // verify that results update failed as attaching outputs failed
    verify(runsDao, never()).updateRunStatusWithError(any(), any(), any(), any());
    verify(wdsService, never())
        .updateRecord(mockRequest, runToUpdate1.runSet().recordType(), runToUpdate1.recordId());
  }

  @Test
  void hasOutputDefinitionReturnsTrue() throws JsonProcessingException {
    assertTrue(runCompletionHandler.hasOutputDefinition(runToUpdate1));
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

    assertFalse(runCompletionHandler.hasOutputDefinition(run));
  }
}
