package bio.terra.cbas.controllers;

import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetDetailsResponse;
import bio.terra.cbas.model.RunSetListResponse;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.WdsRecordSet;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.RunId;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest
@TestPropertySource(properties = "cbas.cbas-api.runSetsMaximumRecordIds=3")
@ContextConfiguration(classes = {RunSetsApiController.class, CbasApiConfiguration.class})
class TestRunSetsApiController {

  private static final String API = "/api/batch/v1/run_sets";
  private final UUID methodId = UUID.randomUUID();
  private final UUID methodVersionId = UUID.randomUUID();
  private final String workflowUrl = "www.example.com/wdls/helloworld.wdl";
  private final String recordType = "MY_RECORD_TYPE";
  private final String recordAttribute = "MY_RECORD_ATTRIBUTE";
  private final String outputDefinitionAsString =
      """
      [ {
        "output_name" : "myWorkflow.myCall.outputName1",
        "output_type" : {
          "type" : "primitive",
          "primitive_type" : "String"
        },
        "record_attribute" : "foo_rating"
      } ]""";

  private final String requestTemplate =
      """
        {
          "method_version_id" : "%s",
          "workflow_input_definitions" : [ {
            "input_name" : "myworkflow.mycall.inputname1",
            "input_type" : { "type": "primitive", "primitive_type": "String" },
            "source" : {
              "type" : "literal",
              "parameter_value" : "literal value"
            }
          }, {
            "input_name" : "myworkflow.mycall.inputname2",
            "input_type" : { "type": "primitive", "primitive_type": "Int" },
            "source" : {
              "type" : "record_lookup",
              "record_attribute" : "MY_RECORD_ATTRIBUTE"
            }
          } ],
          "workflow_output_definitions" : %s,
          "wds_records" : {
            "record_type" : "%s",
            "record_ids" : %s
          }
        }
        """;

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private CromwellService cromwellService;
  @MockBean private WdsService wdsService;
  @MockBean private MethodDao methodDao;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private RunSetDao runSetDao;
  @MockBean private RunDao runDao;
  @MockBean private SmartRunSetsPoller smartRunSetsPoller;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  // The object mapper is pulled from the BeanConfig and used to convert to and from JSON in the
  // tests:
  @Autowired private ObjectMapper objectMapper;

  @Test
  void runSetWith1FailedRunTest() throws Exception {
    // set up mock request
    final String recordId1 = "MY_RECORD_ID_1";
    final String recordId2 = "MY_RECORD_ID_2";
    final String recordId3 = "MY_RECORD_ID_3";
    final int recordAttributeValue1 = 100;
    final int recordAttributeValue2 = 200;
    final int recordAttributeValue3 = 300;
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute, recordAttributeValue1);
    RecordAttributes recordAttributes2 = new RecordAttributes();
    recordAttributes2.put(recordAttribute, recordAttributeValue2);
    RecordAttributes recordAttributes3 = new RecordAttributes();
    recordAttributes3.put(recordAttribute, recordAttributeValue3);
    final String cromwellWorkflowId1 = UUID.randomUUID().toString();
    final String cromwellWorkflowId3 = UUID.randomUUID().toString();
    HashMap<String, Object> workflowInputsMap1 = new HashMap<>();
    workflowInputsMap1.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap1.put("myworkflow.mycall.inputname2", 100L);
    HashMap<String, Object> workflowInputsMap2 = new HashMap<>();
    workflowInputsMap2.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap2.put("myworkflow.mycall.inputname2", 200L);
    HashMap<String, Object> workflowInputsMap3 = new HashMap<>();
    workflowInputsMap3.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap3.put("myworkflow.mycall.inputname2", 300L);
    String request =
        requestTemplate.formatted(
            methodVersionId,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    when(methodDao.getMethod(methodId))
        .thenReturn(
            new Method(
                methodId,
                "methodname",
                "methoddescription",
                OffsetDateTime.now(),
                UUID.randomUUID(),
                "test method source"));

    when(methodVersionDao.getMethodVersion(methodVersionId))
        .thenReturn(
            new MethodVersion(
                methodVersionId,
                new Method(
                    methodId,
                    "methodname",
                    "methoddescription",
                    OffsetDateTime.now(),
                    UUID.randomUUID(),
                    "test method source"),
                "version name",
                "version description",
                OffsetDateTime.now(),
                null,
                workflowUrl));

    // Set up API responses
    when(wdsService.getRecord(recordType, recordId1))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(recordType, recordId2))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId2).attributes(recordAttributes2));
    when(wdsService.getRecord(recordType, recordId3))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId3).attributes(recordAttributes3));

    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap1)))
        .thenReturn(new RunId().runId(cromwellWorkflowId1));
    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap2)))
        .thenThrow(
            new cromwell.client.ApiException(
                "ApiException thrown on purpose for testing purposes."));
    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap3)))
        .thenReturn(new RunId().runId(cromwellWorkflowId3));

    MvcResult result =
        mockMvc
            .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse response =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), RunSetStateResponse.class);

    ArgumentCaptor<RunSet> newRunSetCaptor = ArgumentCaptor.forClass(RunSet.class);
    verify(runSetDao).createRunSet(newRunSetCaptor.capture());
    assertEquals(methodVersionId, newRunSetCaptor.getValue().methodVersion().methodVersionId());
    assertEquals(methodId, newRunSetCaptor.getValue().methodVersion().method().method_id());
    assertEquals(recordType, newRunSetCaptor.getValue().recordType());
    assertEquals(outputDefinitionAsString, newRunSetCaptor.getValue().outputDefinition());

    ArgumentCaptor<Run> newRunCaptor = ArgumentCaptor.forClass(Run.class);
    verify(runDao, times(3)).createRun(newRunCaptor.capture());
    when(runDao.createRun(any())).thenReturn(1);
    List<Run> capturedRuns = newRunCaptor.getAllValues();
    assertEquals(3, capturedRuns.size());
    // check Runs 1 & 3 were successfully submitted
    assertEquals(newRunSetCaptor.getValue().runSetId(), capturedRuns.get(0).getRunSetId());
    assertEquals(cromwellWorkflowId1, capturedRuns.get(0).engineId());
    assertEquals(UNKNOWN, capturedRuns.get(0).status());
    assertEquals(recordId1, capturedRuns.get(0).recordId());
    assertEquals(newRunSetCaptor.getValue().runSetId(), capturedRuns.get(2).getRunSetId());
    assertEquals(cromwellWorkflowId3, capturedRuns.get(2).engineId());
    assertEquals(UNKNOWN, capturedRuns.get(2).status());
    assertEquals(recordId3, capturedRuns.get(2).recordId());
    // check Run 2 is in failed state
    assertEquals(newRunSetCaptor.getValue().runSetId(), capturedRuns.get(1).getRunSetId());
    assertNull(capturedRuns.get(1).engineId());
    assertEquals(SYSTEM_ERROR, capturedRuns.get(1).status());
    assertEquals(recordId2, capturedRuns.get(1).recordId());
    assertThat(
        capturedRuns.get(1).errorMessages(),
        containsString(
            "Cromwell submission failed for Record ID MY_RECORD_ID_2. ApiException: Message: ApiException thrown on purpose for testing purposes"));

    // Assert that the submission timestamp of last Run in set is more recent than 60 seconds ago
    assertThat(
        newRunCaptor.getValue().submissionTimestamp(),
        greaterThan(OffsetDateTime.now().minus(Duration.ofSeconds(60))));
  }

  @Test
  void tooManyRecordIds() throws Exception {
    final String recordIds = "[ \"RECORD1\", \"RECORD2\", \"RECORD3\", \"RECORD4\" ]";
    final int recordAttributeValue = 100;

    String request =
        requestTemplate.formatted(workflowUrl, outputDefinitionAsString, recordType, recordIds);

    mockMvc
        .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError());

    verifyNoInteractions(wdsService);
    verifyNoInteractions(cromwellService);
  }

  @Test
  void wdsRecordIdNotFoundTest() throws Exception {
    final String recordId1 = "MY_RECORD_ID_1";
    final String recordId2 = "MY_RECORD_ID_2";
    final int recordAttributeValue1 = 100;
    final int recordAttributeValue2 = 200;
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute, recordAttributeValue1);

    String request =
        requestTemplate.formatted(
            methodVersionId,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\" ]".formatted(recordId1, recordId2));

    // Set up API responses
    when(wdsService.getRecord(recordType, recordId1))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(recordType, recordId2))
        .thenThrow(
            new org.databiosphere.workspacedata.client.ApiException(
                400, "ApiException thrown for testing purposes."));

    MvcResult result =
        mockMvc
            .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();

    verifyNoInteractions(cromwellService);
    assertThat(
        result.getResponse().getContentAsString(),
        containsString(
            "Error while fetching WDS Records for Record ID(s): {MY_RECORD_ID_2=ApiException thrown for testing purposes.}"));
  }

  @Test
  void getRunSetsApiTest() throws Exception {
    RunSet returnedRunSet1 =
        new RunSet(
            UUID.randomUUID(),
            new MethodVersion(
                UUID.randomUUID(),
                new Method(
                    UUID.randomUUID(),
                    "methodName",
                    "methodDescription",
                    OffsetDateTime.now(),
                    UUID.randomUUID(),
                    "method source"),
                "version name",
                "version description",
                OffsetDateTime.now(),
                UUID.randomUUID(),
                "method url"),
            "",
            "",
            false,
            CbasRunSetStatus.ERROR,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            5,
            1,
            "inputdefinition",
            "outputDefinition",
            "FOO");

    RunSet returnedRunSet2 =
        new RunSet(
            UUID.randomUUID(),
            new MethodVersion(
                UUID.randomUUID(),
                new Method(
                    UUID.randomUUID(),
                    "methodName",
                    "methodDescription",
                    OffsetDateTime.now(),
                    UUID.randomUUID(),
                    "method source"),
                "version name",
                "version description",
                OffsetDateTime.now(),
                UUID.randomUUID(),
                "method url"),
            "",
            "",
            false,
            CbasRunSetStatus.RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            10,
            0,
            "inputdefinition",
            "outputDefinition",
            "BAR");

    List<RunSet> response = List.of(returnedRunSet1, returnedRunSet2);
    when(runSetDao.getRunSets(any())).thenReturn(response);
    when(smartRunSetsPoller.updateRunSets(response)).thenReturn(response);

    MvcResult result =
        mockMvc
            .perform(get(API).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Make sure the runSetsPoller was indeed asked to update the runs:
    verify(smartRunSetsPoller).updateRunSets(response);

    RunSetListResponse parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), RunSetListResponse.class);

    assertEquals(2, parsedResponse.getRunSets().size());

    RunSetDetailsResponse runSetDetails1 = parsedResponse.getRunSets().get(0);
    RunSetDetailsResponse runSetDetails2 = parsedResponse.getRunSets().get(1);

    assertEquals("FOO", runSetDetails1.getRecordType());
    assertEquals(5, runSetDetails1.getRunCount());
    assertEquals(1, runSetDetails1.getErrorCount());
    assertEquals(
        CbasRunSetStatus.toCbasRunSetApiState(CbasRunSetStatus.ERROR), runSetDetails1.getState());

    assertEquals("BAR", runSetDetails2.getRecordType());
    assertEquals(10, runSetDetails2.getRunCount());
    assertEquals(0, runSetDetails2.getErrorCount());
    assertEquals(
        CbasRunSetStatus.toCbasRunSetApiState(CbasRunSetStatus.RUNNING), runSetDetails2.getState());
  }
}

class TestRunSetsApiControllerUnits {
  @Test
  void testRequestValidityFewerThanMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    config.setRunSetsMaximumRecordIds(2);
    request.setWdsRecords(new WdsRecordSet().recordIds(List.of("r1")));
    assertTrue(RunSetsApiController.validateRequestRecordIds(request, config).isEmpty());
  }

  @Test
  void testRequestValidityEqualToMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    config.setRunSetsMaximumRecordIds(2);
    request.setWdsRecords(new WdsRecordSet().recordIds(Arrays.asList("r1", "r2")));
    assertTrue(RunSetsApiController.validateRequestRecordIds(request, config).isEmpty());
  }

  @Test
  void testRequestValidityGreaterThanMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    config.setRunSetsMaximumRecordIds(2);
    request.setWdsRecords(new WdsRecordSet().recordIds(Arrays.asList("r1", "r2", "r3", "r2")));
    List<String> expected =
        Arrays.asList(
            "4 record IDs submitted exceeds the maximum value of 2.",
            "Duplicate Record ID(s) [r2] present in request.");
    List<String> actual = RunSetsApiController.validateRequestRecordIds(request, config);
    assertFalse(actual.isEmpty());
    assertEquals(expected, actual);
  }

  @Test
  void testRequestInputsGreaterThanMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    request.setWorkflowInputDefinitions(
        List.of(new WorkflowInputDefinition(), new WorkflowInputDefinition()));
    request.setWorkflowOutputDefinitions(
        List.of(new WorkflowOutputDefinition(), new WorkflowOutputDefinition()));

    config.setMaxWorkflowInputs(1);
    config.setMaxWorkflowOutputs(5);

    List<String> expectedErrorList =
        List.of("Number of defined inputs (2) exceeds maximum value (1)");
    List<String> actualErrorList =
        RunSetsApiController.validateRequestInputsAndOutputs(request, config);
    assertFalse(actualErrorList.isEmpty());
    assertEquals(expectedErrorList, actualErrorList);
  }

  @Test
  void testRequestOutputsGreaterThanMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    request.setWorkflowInputDefinitions(
        List.of(new WorkflowInputDefinition(), new WorkflowInputDefinition()));
    request.setWorkflowOutputDefinitions(
        List.of(new WorkflowOutputDefinition(), new WorkflowOutputDefinition()));

    config.setMaxWorkflowInputs(5);
    config.setMaxWorkflowOutputs(1);

    List<String> expectedErrorList =
        List.of("Number of defined outputs (2) exceeds maximum value (1)");
    List<String> actualErrorList =
        RunSetsApiController.validateRequestInputsAndOutputs(request, config);
    assertFalse(actualErrorList.isEmpty());
    assertEquals(expectedErrorList, actualErrorList);
  }

  @Test
  void testRequestInputsAndOutputsGreaterThanMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    request.setWorkflowInputDefinitions(
        List.of(new WorkflowInputDefinition(), new WorkflowInputDefinition()));
    request.setWorkflowOutputDefinitions(
        List.of(new WorkflowOutputDefinition(), new WorkflowOutputDefinition()));

    config.setMaxWorkflowInputs(1);
    config.setMaxWorkflowOutputs(1);

    String expectedOutputError = "Number of defined outputs (2) exceeds maximum value (1)";
    String expectedInputError = "Number of defined inputs (2) exceeds maximum value (1)";
    List<String> actualErrorList =
        RunSetsApiController.validateRequestInputsAndOutputs(request, config);
    assertFalse(actualErrorList.isEmpty());
    assertTrue(actualErrorList.contains(expectedOutputError));
    assertTrue(actualErrorList.contains(expectedInputError));
    assertEquals(2, actualErrorList.size());
  }

  @Test
  void testRequestInputsAndOutputsEqualToMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    request.setWorkflowInputDefinitions(
        List.of(new WorkflowInputDefinition(), new WorkflowInputDefinition()));
    request.setWorkflowOutputDefinitions(
        List.of(new WorkflowOutputDefinition(), new WorkflowOutputDefinition()));

    config.setMaxWorkflowInputs(2);
    config.setMaxWorkflowOutputs(2);

    List<String> actualErrorList =
        RunSetsApiController.validateRequestInputsAndOutputs(request, config);
    assertTrue(actualErrorList.isEmpty());
  }
}
