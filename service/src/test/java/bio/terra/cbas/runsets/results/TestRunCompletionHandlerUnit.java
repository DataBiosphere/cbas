package bio.terra.cbas.runsets.results;

import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.common.exceptions.OutputProcessingException;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.types.CoercionException;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunCompletionHandler.class)
class TestRunCompletionHandlerUnit {

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

  private static final UUID workspaceId = UUID.randomUUID();

  public ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .setDateFormat(new StdDateFormat())
          .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
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
                  "method source",
                  workspaceId),
              "version name",
              "version description",
              methodCreatedTime,
              runSetId,
              "file:///method/source/url",
              workspaceId),
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
          "user-foo",
          workspaceId);

  private static final RunSet runSetNoOutputs =
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
                  "method source",
                  workspaceId),
              "version name",
              "version description",
              methodCreatedTime,
              runSetId,
              "file:///method/source/url",
              workspaceId),
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
          "[]",
          "entityType",
          "user-foo",
          workspaceId);

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
          runSetNoOutputs,
          runningRunEntityId2,
          runSubmittedTime,
          UNKNOWN,
          runningRunStatusUpdateTime,
          runningRunStatusUpdateTime,
          errorMessages);

  @BeforeEach
  public void init() {
    RunDao runsDao = mock(RunDao.class);
    WdsService wdsService = mock(WdsService.class);
    MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);
    runCompletionHandler =
        new RunCompletionHandler(runsDao, wdsService, objectMapper, micrometerMetrics);
  }

  @Test
  void buildRecordAttributesFromWorkflowOutputsSucceeds()
      throws OutputProcessingException, CoercionException, JsonProcessingException {
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
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

    var recordAttributes =
        runCompletionHandler.buildRecordAttributesFromWorkflowOutputs(
            runToUpdate1, cromwellOutputs);

    assertEquals(mockAttributes, recordAttributes);
  }

  @Test
  void buildRecordAttributesFromWorkflowOutputsReturnEmptyMap()
      throws OutputProcessingException, CoercionException, JsonProcessingException {
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    String rawOutputs =
        """
        {
            "outputs": {
              "wf_hello.hello.salutation": "Hello batch!"
            }
          }
        """;
    Object cromwellOutputs = object.fromJson(rawOutputs, RunLog.class).getOutputs();

    var recordAttributes =
        runCompletionHandler.buildRecordAttributesFromWorkflowOutputs(
            runToUpdate2, cromwellOutputs);

    assertTrue(recordAttributes.isEmpty());
  }

  @Test
  void buildRecordAttributesFromWorkflowOutputsThrowsMisspelledOutputs() {
    // Using Gson here since Cromwell client uses it to interpret runLogValue into Java objects.
    Gson object = new Gson();
    String misspelledRawOutputs =
        """
        {
            "outputs": {
              "wf_hello.hello.salutationz": "Hello batch!"
            }
          }
        """;

    Object cromwellOutputs = object.fromJson(misspelledRawOutputs, RunLog.class).getOutputs();

    Assertions.assertThrows(
        OutputProcessingException.class,
        () ->
            runCompletionHandler.buildRecordAttributesFromWorkflowOutputs(
                runToUpdate1, cromwellOutputs));
  }

  @Test
  void hasOutputDefinitionReturnsTrue() throws JsonProcessingException {
    assertTrue(runCompletionHandler.hasOutputDefinition(runToUpdate1));
  }

  @Test
  void hasOutputDefinitionReturnsFalse() throws JsonProcessingException {
    assertFalse(runCompletionHandler.hasOutputDefinition(runToUpdate2));
  }
}
