package bio.terra.cbas.controllers;

import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.RunId;
import java.time.Duration;
import java.time.OffsetDateTime;
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
  void chainCallsTogether() throws Exception {

    final String workflowUrl = "www.example.com/wdls/helloworld.wdl";
    final String recordType = "MY_RECORD_TYPE";
    final String recordId = "MY_RECORD_ID";
    final String recordAttribute = "MY_RECORD_ATTRIBUTE";
    final int recordAttributeValue = 100;
    RecordAttributes recordAttributes = new RecordAttributes();
    recordAttributes.put(recordAttribute, recordAttributeValue);
    final String cromwellWorkflowId = UUID.randomUUID().toString();

    // Set up API responses:
    when(wdsService.getRecord(recordType, recordId))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId).attributes(recordAttributes));

    when(cromwellService.submitWorkflow(eq(workflowUrl), any()))
        .thenReturn(new RunId().runId(cromwellWorkflowId));

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
          "wds_records" : {
            "record_type" : "%s",
            "record_ids" : [ "%s" ]
          }
        }
        """
            .formatted(workflowUrl, recordType, recordId);

    MvcResult result =
        mockMvc
            .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse response =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), RunSetStateResponse.class);

    // Verify database storage:
    ArgumentCaptor<Method> newMethodCaptor = ArgumentCaptor.forClass(Method.class);
    verify(methodDao).createMethod(newMethodCaptor.capture());
    assertEquals(recordType, newMethodCaptor.getValue().recordType());
    assertEquals(workflowUrl, newMethodCaptor.getValue().methodUrl());

    ArgumentCaptor<RunSet> newRunSetCaptor = ArgumentCaptor.forClass(RunSet.class);
    verify(runSetDao).createRunSet(newRunSetCaptor.capture());
    assertEquals(newMethodCaptor.getValue().id(), newRunSetCaptor.getValue().getMethodId());

    ArgumentCaptor<Run> newRunCaptor = ArgumentCaptor.forClass(Run.class);
    verify(runDao).createRun(newRunCaptor.capture());
    when(runDao.createRun(any())).thenReturn(1);
    assertEquals(newRunSetCaptor.getValue().id(), newRunCaptor.getValue().getRunSetId());
    assertEquals(cromwellWorkflowId, newRunCaptor.getValue().engineId());
    assertEquals(UNKNOWN, newRunCaptor.getValue().status());
    assertEquals(recordId, newRunCaptor.getValue().recordId());
    
    // Assert that the submission timestamp is more recent than 60 seconds ago
    assertThat(
        newRunCaptor.getValue().submissionTimestamp(),
        greaterThan(OffsetDateTime.now().minus(Duration.ofSeconds(60))));
  }
}
