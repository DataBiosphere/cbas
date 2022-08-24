package bio.terra.cbas.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.cromwell.CromwellService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.WorkflowIdAndStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.databiosphere.workspacedata.model.EntityAttributes;
import org.databiosphere.workspacedata.model.EntityResponse;
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
public class TestRunSetsApiController {

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
    final String entityType = "MY_ENTITY_TYPE";
    final String entityId = "MY_ENTITY_ID";
    final String entityAttribute = "MY_ENTITY_ATTRIBUTE";
    final int entityAttributeValue = 100;
    EntityAttributes entityAttributes = new EntityAttributes();
    entityAttributes.put(entityAttribute, entityAttributeValue);
    final String cromwellWorkflowId = UUID.randomUUID().toString();
    final String cromwellWorkflowStatus = "Submitted";

    // Set up API responses:
    when(wdsService.getEntity(entityType, entityId))
        .thenReturn(
            new EntityResponse().type(entityType).id(entityId).attributes(entityAttributes));

    when(cromwellService.submitWorkflow(eq(workflowUrl), any()))
        .thenReturn(
            new WorkflowIdAndStatus().id(cromwellWorkflowId).status(cromwellWorkflowStatus));

    String request =
        """
        {
          "workflow_url" : "%s",
          "workflow_param_definitions" : [ {
            "parameter_name" : "myworkflow.mycall.inputname1",
            "parameter_type" : "String",
            "source" : {
              "type" : "literal",
              "parameter_value" : "literal value"
            }
          }, {
            "parameter_name" : "myworkflow.mycall.inputname2",
            "parameter_type" : "Int",
            "source" : {
              "type" : "entity_lookup",
              "entity_attribute" : "MY_ENTITY_ATTRIBUTE"
            }
          } ],
          "wds_entities" : {
            "entity_type" : "%s",
            "entity_ids" : [ "%s" ]
          }
        }
        """
            .formatted(workflowUrl, entityType, entityId);

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
    assertEquals(entityType, newMethodCaptor.getValue().entityType());
    assertEquals(workflowUrl, newMethodCaptor.getValue().methodUrl());

    ArgumentCaptor<RunSet> newRunSetCaptor = ArgumentCaptor.forClass(RunSet.class);
    verify(runSetDao).createRunSet(newRunSetCaptor.capture());
    assertEquals(newMethodCaptor.getValue().id(), newRunSetCaptor.getValue().methodId());

    ArgumentCaptor<Run> newRunCaptor = ArgumentCaptor.forClass(Run.class);
    verify(runDao).createRun(newRunCaptor.capture());
    assertEquals(newRunSetCaptor.getValue().id(), newRunCaptor.getValue().runSetId());
    assertEquals(cromwellWorkflowId, newRunCaptor.getValue().engineId());
    assertEquals(cromwellWorkflowStatus, newRunCaptor.getValue().status());
    assertEquals(entityId, newRunCaptor.getValue().entityId());
    // Assert that the submission timestamp is more recent than 60 seconds ago
    assertTrue(
        newRunCaptor
                .getValue()
                .submissionTimestamp()
                .compareTo(OffsetDateTime.now().minus(Duration.ofSeconds(60)))
            > 0);
  }
}
