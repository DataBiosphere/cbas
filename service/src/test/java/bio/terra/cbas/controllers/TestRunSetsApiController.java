package bio.terra.cbas.controllers;

import static bio.terra.cbas.controllers.RunSetsApiController.checkInvalidRequest;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.util.Optional;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest
@ContextConfiguration(classes = RunSetsApiController.class)
class TestRunSetsApiController {

  private static final String API = "/api/batch/v1/run_sets";

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
  void testSubmissionOf2Runs() throws Exception {
    // set up mock request
    final String workflowUrl = "www.example.com/wdls/helloworld.wdl";
    final String recordType = "MY_RECORD_TYPE";
    final String recordId1 = "MY_RECORD_ID_1";
    final String recordId2 = "MY_RECORD_ID_2";
    final String recordAttribute = "MY_RECORD_ATTRIBUTE";
    final int recordAttributeValue1 = 100;
    final int recordAttributeValue2 = 200;
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute, recordAttributeValue1);
    RecordAttributes recordAttributes2 = new RecordAttributes();
    recordAttributes2.put(recordAttribute, recordAttributeValue2);
    final String cromwellWorkflowId1 = UUID.randomUUID().toString();
    final String cromwellWorkflowId2 = UUID.randomUUID().toString();
    HashMap<String, Object> workflowInputsMap1 = new HashMap<>();
    workflowInputsMap1.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap1.put("myworkflow.mycall.inputname2", 100);
    HashMap<String, Object> workflowInputsMap2 = new HashMap<>();
    workflowInputsMap2.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap2.put("myworkflow.mycall.inputname2", 200);
    final String outputDefinitionAsString =
        """
        [ {
          "output_name" : "myWorkflow.myCall.outputName1",
          "output_type" : "String",
          "record_attribute" : "foo_rating"
        } ]""";
    String request =
        """
        {
          "workflow_url" : "%s",
          "workflow_input_definitions" : [ {
            "input_name" : "myworkflow.mycall.inputname1",
            "input_type" : "String",
            "source" : {
              "type" : "literal",
              "parameter_value" : "literal value"
            }
          }, {
            "input_name" : "myworkflow.mycall.inputname2",
            "input_type" : "Int",
            "source" : {
              "type" : "record_lookup",
              "record_attribute" : "MY_RECORD_ATTRIBUTE"
            }
          } ],
          "workflow_output_definitions" : %s,
          "wds_records" : {
            "record_type" : "%s",
            "record_ids" : [ "%s", "%s" ]
          }
        }
        """
            .formatted(workflowUrl, outputDefinitionAsString, recordType, recordId1, recordId2);

    // Set up API responses
    when(wdsService.getRecord(recordType, recordId1))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(recordType, recordId2))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId2).attributes(recordAttributes2));

    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap1)))
        .thenReturn(new RunId().runId(cromwellWorkflowId1));
    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap2)))
        .thenReturn(new RunId().runId(cromwellWorkflowId2));

    // submit request
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
    verify(runDao, times(2)).createRun(newRunCaptor.capture());
    when(runDao.createRun(any())).thenReturn(1);
    List<Run> capturedRuns = newRunCaptor.getAllValues();
    assertEquals(2, capturedRuns.size());
    assertEquals(newRunSetCaptor.getValue().id(), capturedRuns.get(0).getRunSetId());
    assertEquals(cromwellWorkflowId1, capturedRuns.get(0).engineId());
    assertEquals(UNKNOWN, capturedRuns.get(0).status());
    assertEquals(recordId1, capturedRuns.get(0).recordId());
    assertEquals(newRunSetCaptor.getValue().id(), capturedRuns.get(1).getRunSetId());
    assertEquals(cromwellWorkflowId2, capturedRuns.get(1).engineId());
    assertEquals(UNKNOWN, capturedRuns.get(1).status());
    assertEquals(recordId2, capturedRuns.get(1).recordId());

    // Assert that the submission timestamp os last Run in set is more recent than 60 seconds ago
    assertThat(
        newRunCaptor.getValue().submissionTimestamp(),
        greaterThan(OffsetDateTime.now().minus(Duration.ofSeconds(60))));
  }

  @Test
  void checkInvalidRequestTest() {
    // in this test we are only testing WDS Record IDs criteria and hence ignoring other parts of
    // request
    List<String> recordIds = Arrays.asList("FOO1", "FOO2", "FOO3", "FOO2");
    WdsRecordSet wdsRecordSet = new WdsRecordSet();
    wdsRecordSet.setRecordType("FOO");
    wdsRecordSet.setRecordIds(recordIds);
    RunSetRequest invalidRequest = new RunSetRequest();
    invalidRequest.setWdsRecords(wdsRecordSet);
    String expectedErrorMsg =
        "Bad user request. Error(s): Current support is exactly one record per request. Duplicate Record ID(s) [FOO2] present in request.";

    Optional<ResponseEntity<RunSetStateResponse>> validationResponse =
        checkInvalidRequest(invalidRequest);
    assertNotNull(validationResponse);
    assertEquals(HttpStatus.BAD_REQUEST, validationResponse.get().getStatusCode());
    assertEquals(expectedErrorMsg, validationResponse.get().getBody().getErrors());
  }

  @Test
  void checkValidRequestTest() {
    // in this test we are only testing WDS Record IDs criteria and hence ignoring other parts of
    // request
    List<String> recordIds = Arrays.asList("FOO1", "FOO2");
    WdsRecordSet wdsRecordSet = new WdsRecordSet();
    wdsRecordSet.setRecordType("FOO");
    wdsRecordSet.setRecordIds(recordIds);
    RunSetRequest invalidRequest = new RunSetRequest();
    invalidRequest.setWdsRecords(wdsRecordSet);

    Optional<ResponseEntity<RunSetStateResponse>> validationResponse =
        checkInvalidRequest(invalidRequest);
    assertEquals(Optional.empty(), validationResponse);
  }
}
