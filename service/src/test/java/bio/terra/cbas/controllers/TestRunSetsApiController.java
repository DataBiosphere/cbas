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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.WdsRecordSet;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
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

  private String requestTemplate =
      """
        {
          "workflow_url" : "%s",
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
  @MockBean private RunSetDao runSetDao;
  @MockBean private RunDao runDao;

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
    workflowInputsMap1.put("myworkflow.mycall.inputname2", 100);
    HashMap<String, Object> workflowInputsMap2 = new HashMap<>();
    workflowInputsMap2.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap2.put("myworkflow.mycall.inputname2", 200);
    HashMap<String, Object> workflowInputsMap3 = new HashMap<>();
    workflowInputsMap3.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap3.put("myworkflow.mycall.inputname2", 300);
    String request =
        requestTemplate.formatted(
            workflowUrl,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

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

    // Verify database storage
    ArgumentCaptor<Method> newMethodCaptor = ArgumentCaptor.forClass(Method.class);
    verify(methodDao).createMethod(newMethodCaptor.capture());
    assertEquals(recordType, newMethodCaptor.getValue().recordType());
    assertEquals(workflowUrl, newMethodCaptor.getValue().methodUrl());
    assertEquals(outputDefinitionAsString, newMethodCaptor.getValue().outputDefinition());

    ArgumentCaptor<RunSet> newRunSetCaptor = ArgumentCaptor.forClass(RunSet.class);
    verify(runSetDao).createRunSet(newRunSetCaptor.capture());
    assertEquals(newMethodCaptor.getValue().id(), newRunSetCaptor.getValue().getMethodId());

    ArgumentCaptor<Run> newRunCaptor = ArgumentCaptor.forClass(Run.class);
    verify(runDao, times(3)).createRun(newRunCaptor.capture());
    when(runDao.createRun(any())).thenReturn(1);
    List<Run> capturedRuns = newRunCaptor.getAllValues();
    assertEquals(3, capturedRuns.size());
    // check Runs 1 & 3 were successfully submitted
    assertEquals(newRunSetCaptor.getValue().id(), capturedRuns.get(0).getRunSetId());
    assertEquals(cromwellWorkflowId1, capturedRuns.get(0).engineId());
    assertEquals(UNKNOWN, capturedRuns.get(0).status());
    assertEquals(recordId1, capturedRuns.get(0).recordId());
    assertEquals(newRunSetCaptor.getValue().id(), capturedRuns.get(2).getRunSetId());
    assertEquals(cromwellWorkflowId3, capturedRuns.get(2).engineId());
    assertEquals(UNKNOWN, capturedRuns.get(2).status());
    assertEquals(recordId3, capturedRuns.get(2).recordId());
    // check Run 2 is in failed state
    assertEquals(newRunSetCaptor.getValue().id(), capturedRuns.get(1).getRunSetId());
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
    RecordAttributes recordAttributes = new RecordAttributes();
    recordAttributes.put(recordAttribute, recordAttributeValue);

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
    RecordAttributes recordAttributes2 = new RecordAttributes();
    recordAttributes2.put(recordAttribute, recordAttributeValue2);

    String request =
        requestTemplate.formatted(
            workflowUrl,
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
}

class UnitTestRunSetsApiController {
  private CbasApiConfiguration config = new CbasApiConfiguration();
  private RunSetRequest request = new RunSetRequest();

  @Test
  void testRequestValidityFewerThanMax() {
    config.setRunSetsMaximumRecordIds(2);
    request.setWdsRecords(new WdsRecordSet().recordIds(Arrays.asList("r1")));
    assertTrue(RunSetsApiController.validateRequest(request, config).isEmpty());
  }

  @Test
  void testRequestValidityEqualToMax() {
    config.setRunSetsMaximumRecordIds(2);
    request.setWdsRecords(new WdsRecordSet().recordIds(Arrays.asList("r1", "r2")));
    assertTrue(RunSetsApiController.validateRequest(request, config).isEmpty());
  }

  @Test
  void testRequestValidityGreaterThanMax() {
    config.setRunSetsMaximumRecordIds(2);
    request.setWdsRecords(new WdsRecordSet().recordIds(Arrays.asList("r1", "r2", "r3", "r2")));
    List<String> expected =
        Arrays.asList(
            "4 record IDs submitted exceeds the maximum value of 2.",
            "Duplicate Record ID(s) [r2] present in request.");
    List<String> actual = RunSetsApiController.validateRequest(request, config);
    assertFalse(actual.isEmpty());
    assertEquals(expected, actual);
  }
}
