package bio.terra.cbas.controllers;

import static bio.terra.cbas.models.CbasRunStatus.COMPLETE;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.model.RunLog;
import bio.terra.cbas.model.RunLogResponse;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater.UpdateResult;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest
@ContextConfiguration(classes = RunsApiController.class)
class TestRunsApiController {

  private static final String API = "/api/batch/v1/runs";

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private RunDao runDao;
  // The smart poller does most of the clever update logic, so we can test that separately. This
  // test just needs to make sure we call it properly and respect its updates
  @MockBean private SmartRunsPoller smartRunsPoller;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  // The object mapper is pulled from the BeanConfig and used to convert to and from JSON in the
  // tests:
  @Autowired private ObjectMapper objectMapper;

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
          false,
          CbasRunSetStatus.UNKNOWN,
          returnedSubmittedTime,
          returnedSubmittedTime,
          returnedSubmittedTime,
          0,
          0,
          "inputDefinition",
          "outputDefinition",
          "entitytype");

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
}
