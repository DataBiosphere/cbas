package bio.terra.cbas.controllers;

import static bio.terra.cbas.models.CbasRunStatus.CANCELING;
import static bio.terra.cbas.models.CbasRunStatus.RUNNING;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static bio.terra.cbas.models.CbasRunStatus.UNKNOWN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.config.AzureCredentialConfig;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.leonardo.AppUtils;
import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import bio.terra.cbas.dependencies.sam.SamClient;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.cbas.dependencies.wes.CromwellClient;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.AbortRunSetResponse;
import bio.terra.cbas.model.ErrorReport;
import bio.terra.cbas.model.OutputDestination;
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
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager.AbortRequestDetails;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import bio.terra.cbas.util.UuidSource;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import bio.terra.dockstore.model.ToolDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.RunId;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.BeanCreationException;
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
@ContextConfiguration(
    classes = {
      RunSetsApiController.class,
      CbasApiConfiguration.class,
      GlobalExceptionHandler.class
    })
class TestRunSetsApiController {

  private static final String API = "/api/batch/v1/run_sets";
  private static final String API_ABORT = "/api/batch/v1/run_sets/abort";
  private final UUID methodId = UUID.randomUUID();
  private final UUID methodVersionId = UUID.randomUUID();
  private final UUID dockstoreMethodVersionId = UUID.randomUUID();
  private final String workflowUrl =
      "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl";
  private final Boolean isCallCachingEnabled = false;
  private final String dockstoreWorkflowUrl = "github.com/broadinstitute/cromwell/hello.wdl";
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
        "destination" : {
          "type" : "record_update",
          "record_attribute" : "foo_rating"
        }
      } ]""";

  private final String requestTemplate =
      """
        {
          "method_version_id" : "%s",
          "call_caching_enabled": "%s",
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
          }, {
            "input_name" : "myworkflow.mycall.inputname3",
            "input_type" : { "type": "optional", "optional_type": { "type": "primitive", "primitive_type": "Int" }},
            "source" : %s
          } ],
          "workflow_output_definitions" : %s,
          "wds_records" : {
            "record_type" : "%s",
            "record_ids" : %s
          }
        }
        """;

  private final String requestTemplate2 =
      """
        {
          "method_version_id" : "%s",
          "call_caching_enabled": "%s",
          "workflow_input_definitions" : [ %s
          {
            "input_name" : "myworkflow.mycall.inputname1",
            "input_type" : { "type": "primitive", "primitive_type": "String" },
            "source" : {
              "type" : "literal",
              "parameter_value" : "literal value"
            }
          }],
          "workflow_output_definitions" : [ %s
          {
            "output_name" : "myWorkflow.myCall.outputName1",
            "output_type" : {
              "type" : "primitive",
              "primitive_type" : "String"
            },
            "destination" : {
              "type" : "record_update",
              "record_attribute" : "foo_rating"
            }
          }],
          "wds_records" : {
            "record_type" : "%s",
            "record_ids" : %s
          }
        }
        """;

  final String recordId1 = "MY_RECORD_ID_1";
  final String recordId2 = "MY_RECORD_ID_2";
  final String recordId3 = "MY_RECORD_ID_3";

  final String cromwellWorkflowId1 = UUID.randomUUID().toString();
  final String cromwellWorkflowId3 = UUID.randomUUID().toString();

  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private BearerToken bearerToken;
  @MockBean private SamClient samClient;
  @MockBean private UsersApi usersApi;
  @MockBean private SamService samService;
  @MockBean private CromwellService cromwellService;
  @MockBean private WdsService wdsService;
  @MockBean private DockstoreService dockstoreService;
  @MockBean private MethodDao methodDao;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private RunSetDao runSetDao;
  @MockBean private RunDao runDao;
  @MockBean private SmartRunSetsPoller smartRunSetsPoller;
  @MockBean private UuidSource uuidSource;
  @MockBean private RunSetAbortManager abortManager;
  @Mock private LeonardoService leonardoService;
  @Mock private AppUtils appUtils;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  // The object mapper is pulled from the BeanConfig and used to convert to and from JSON in the
  // tests:
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setupFunctionalChecks() throws Exception {
    // set up mock request
    final int recordAttributeValue1 = 100;
    final int recordAttributeValue2 = 200;
    final int recordAttributeValue3 = 300;
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute, recordAttributeValue1);
    RecordAttributes recordAttributes2 = new RecordAttributes();
    recordAttributes2.put(recordAttribute, recordAttributeValue2);
    RecordAttributes recordAttributes3 = new RecordAttributes();
    recordAttributes3.put(recordAttribute, recordAttributeValue3);

    HashMap<String, Object> workflowInputsMap1 = new HashMap<>();
    workflowInputsMap1.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap1.put("myworkflow.mycall.inputname2", 100L);
    HashMap<String, Object> workflowInputsMap2 = new HashMap<>();
    workflowInputsMap2.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap2.put("myworkflow.mycall.inputname2", 200L);
    HashMap<String, Object> workflowInputsMap3 = new HashMap<>();
    workflowInputsMap3.put("myworkflow.mycall.inputname1", "literal value");
    workflowInputsMap3.put("myworkflow.mycall.inputname2", 300L);

    ToolDescriptor mockToolDescriptor = new ToolDescriptor();
    mockToolDescriptor.setDescriptor("mock descriptor");
    mockToolDescriptor.setType(ToolDescriptor.TypeEnum.WDL);
    mockToolDescriptor.setUrl(workflowUrl);

    when(methodDao.getMethod(methodId))
        .thenReturn(
            new Method(
                methodId,
                "methodname",
                "methoddescription",
                OffsetDateTime.now(),
                UUID.randomUUID(),
                "GitHub"));

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
                    "GitHub"),
                "version name",
                "version description",
                OffsetDateTime.now(),
                null,
                workflowUrl));

    when(methodVersionDao.getMethodVersion(dockstoreMethodVersionId))
        .thenReturn(
            new MethodVersion(
                dockstoreMethodVersionId,
                new Method(
                    methodId,
                    "dockstore method name",
                    "dockstore method description",
                    OffsetDateTime.now(),
                    UUID.randomUUID(),
                    "Dockstore"),
                "develop",
                "version description",
                OffsetDateTime.now(),
                null,
                dockstoreWorkflowUrl));

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

    when(dockstoreService.descriptorGetV1(dockstoreWorkflowUrl, "develop"))
        .thenReturn(mockToolDescriptor);

    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap1), any()))
        .thenReturn(new RunId().runId(cromwellWorkflowId1));
    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap2), any()))
        .thenThrow(
            new cromwell.client.ApiException(
                "ApiException thrown on purpose for testing purposes."));
    when(cromwellService.submitWorkflow(eq(workflowUrl), eq(workflowInputsMap3), any()))
        .thenReturn(new RunId().runId(cromwellWorkflowId3));

    doReturn(mockUser).when(samService).getSamUser();

    // setup Sam permission check to return true
    when(samService.hasReadPermission()).thenReturn(true);
    when(samService.hasComputePermission()).thenReturn(true);
  }

  @Test
  void runSetWithSamException() throws Exception {
    final String optionalInputSourceString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String request =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            optionalInputSourceString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    doCallRealMethod().when(samService).getSamUser();
    doReturn(usersApi).when(samService).getUsersApi();
    when(usersApi.getUserStatusInfo()).thenThrow(new ApiException(401, "No token provided"));

    mockMvc
        .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof SamUnauthorizedException))
        .andExpect(
            result ->
                assertEquals(
                    "Error getting user status info from Sam: No token provided",
                    result.getResolvedException().getMessage()));
  }

  @Test
  void runSetWith1FailedRunTest() throws Exception {
    final String optionalInputSourceString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String request =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            optionalInputSourceString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

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
    assertEquals(methodId, newRunSetCaptor.getValue().methodVersion().method().methodId());
    assertEquals(recordType, newRunSetCaptor.getValue().recordType());
    assertEquals(outputDefinitionAsString, newRunSetCaptor.getValue().outputDefinition());
    assertEquals(isCallCachingEnabled, newRunSetCaptor.getValue().callCachingEnabled());
    assertEquals(mockUser.getUserSubjectId(), newRunSetCaptor.getValue().userId());

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
  void postRunSetRequestForDockstoreWorkflow() throws Exception {
    final String optionalInputSourceString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String request =
        requestTemplate.formatted(
            dockstoreMethodVersionId,
            isCallCachingEnabled,
            optionalInputSourceString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\" ]".formatted(recordId1));

    when(runDao.createRun(any())).thenReturn(1);

    MvcResult result =
        mockMvc
            .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse response =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), RunSetStateResponse.class);

    // verify dockstoreService and cromwellService methods were called with expected params
    verify(dockstoreService).descriptorGetV1(dockstoreWorkflowUrl, "develop");
    verify(cromwellService).submitWorkflow(eq(workflowUrl), any(), any());

    assertNull(response.getErrors());
  }

  @Test
  void postRunSetRequestTooManyInputs() throws Exception {
    String optionalOutputSourceString = "";
    String twoHundredInputs =
        """
          {
            "input_name" : "myworkflow.mycall.inputname1",
            "input_type" : { "type": "primitive", "primitive_type": "String" },
            "source" : {
              "type" : "literal",
              "parameter_value" : "literal value"
            }
        },
        """
            .repeat(200);

    String request =
        requestTemplate2.formatted(
            methodVersionId,
            false,
            twoHundredInputs,
            optionalOutputSourceString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    when(runDao.createRun(any())).thenReturn(1);

    MvcResult result =
        mockMvc
            .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse response =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), RunSetStateResponse.class);

    assertEquals(
        "Bad user request. Error(s): [Number of defined inputs (201) exceeds maximum value (200)]",
        response.getErrors());
  }

  @Test
  void postRunSetRequestTooManyOutputs() throws Exception {
    String optionalInputSourceString = "";
    String threeHundredOutputs =
        """
          {
            "output_name" : "myWorkflow.myCall.outputName1",
            "output_type" : {
              "type" : "primitive",
              "primitive_type" : "String"
            },
            "destination" : {
              "type" : "record_update",
              "record_attribute" : "foo_rating"
            }
          },
        """
            .repeat(300);

    String request =
        requestTemplate2.formatted(
            methodVersionId,
            isCallCachingEnabled,
            optionalInputSourceString,
            threeHundredOutputs,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    when(runDao.createRun(any())).thenReturn(1);

    MvcResult result =
        mockMvc
            .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse response =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), RunSetStateResponse.class);

    assertEquals(
        "Bad user request. Error(s): [Number of defined outputs (301) exceeds maximum value (300)]",
        response.getErrors());
  }

  @Test
  void runSetOptionalSourceNone() throws Exception {
    String inputSourceAsString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String requestOptionalNone =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            inputSourceAsString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    MvcResult resultOptionalNone =
        mockMvc
            .perform(post(API).content(requestOptionalNone).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse responseOptionalNone =
        objectMapper.readValue(
            resultOptionalNone.getResponse().getContentAsString(), RunSetStateResponse.class);
  }

  @Test
  void runSetOptionalSourceRecordLookup() throws Exception {
    String inputSourceAsString = "{ \"type\" : \"record_lookup\", \"record_attribute\" : 101 }";
    String requestOptionalRecordLookup =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            inputSourceAsString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    MvcResult resultOptionalRecordLookup =
        mockMvc
            .perform(
                post(API)
                    .content(requestOptionalRecordLookup)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse responseOptionalRecordLookup =
        objectMapper.readValue(
            resultOptionalRecordLookup.getResponse().getContentAsString(),
            RunSetStateResponse.class);
  }

  @Test
  void runSetOptionalSourceLiteral() throws Exception {
    String inputSourceAsString = "{ \"type\" : \"literal\", \"value\" : 102 }";
    String requestOptionalLiteral =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            inputSourceAsString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    MvcResult resultOptionalLiteral =
        mockMvc
            .perform(
                post(API).content(requestOptionalLiteral).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // Validate that the response can be parsed as a valid RunSetStateResponse:
    RunSetStateResponse responseOptionalLiteral =
        objectMapper.readValue(
            resultOptionalLiteral.getResponse().getContentAsString(), RunSetStateResponse.class);
  }

  @Test
  void tooManyRecordIds() throws Exception {
    final String recordIds = "[ \"RECORD1\", \"RECORD2\", \"RECORD3\", \"RECORD4\" ]";
    String inputSourceAsString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String request =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            inputSourceAsString,
            outputDefinitionAsString,
            recordType,
            recordIds);

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
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute, recordAttributeValue1);

    String inputSourceAsString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String request =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            inputSourceAsString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\" ]".formatted(recordId1, recordId2));

    // Set up API responses
    when(wdsService.getRecord(recordType, recordId1))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(recordType, recordId2))
        .thenThrow(
            new WdsServiceApiException(
                new org.databiosphere.workspacedata.client.ApiException(
                    400, "ApiException thrown for testing purposes.")));

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
  void testWorkflowOptionsProperlyConstructed() {
    CromwellServerConfiguration localTestConfig =
        new CromwellServerConfiguration("my/base/uri", "my/final/workflow/log/dir", false);
    var leonardoServerConfiguration =
        new LeonardoServerConfiguration("", List.of(), Duration.ofMinutes(10), false);
    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);
    var azureCredentialConfig =
        new AzureCredentialConfig(Duration.ZERO, Duration.ofMillis(100), null);
    CredentialLoader credentialLoader = new CredentialLoader(azureCredentialConfig);
    CromwellClient localTestClient =
        new CromwellClient(localTestConfig, dependencyUrlLoader, credentialLoader);
    CromwellService localtestService = new CromwellService(localTestClient, localTestConfig);

    // Workflow options should reflect the final workflow log directory.
    // write_to_cache should always be true. read_from_cache should match the provided call caching
    // option.
    String expected =
        "{\"final_workflow_log_dir\":\"my/final/workflow/log/dir\",\"read_from_cache\":true,\"write_to_cache\":true}";
    assertEquals(expected, localtestService.buildWorkflowOptionsJson(true));
    String expectedFalse =
        "{\"final_workflow_log_dir\":\"my/final/workflow/log/dir\",\"read_from_cache\":false,\"write_to_cache\":true}";
    assertEquals(expectedFalse, localtestService.buildWorkflowOptionsJson(false));
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
            false,
            CbasRunSetStatus.ERROR,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            5,
            1,
            "inputdefinition",
            "outputDefinition",
            "FOO",
            mockUser.getUserSubjectId());

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
            false,
            CbasRunSetStatus.RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            10,
            0,
            "inputdefinition",
            "outputDefinition",
            "BAR",
            mockUser.getUserSubjectId());

    List<RunSet> response = List.of(returnedRunSet1, returnedRunSet2);
    when(runSetDao.getRunSets(any(), eq(false))).thenReturn(response);
    when(smartRunSetsPoller.updateRunSets(response))
        .thenReturn(new TimeLimitedUpdater.UpdateResult<>(response, 2, 2, true));

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
    assertEquals(true, parsedResponse.isFullyUpdated());

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

  @Test
  void testRunSetAbort() throws Exception {
    RunSet returnedRunSet1Running =
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
            false,
            CbasRunSetStatus.RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            2,
            0,
            "inputdefinition",
            "outputDefinition",
            "FOO",
            mockUser.getUserSubjectId());

    Run run1 =
        new Run(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            returnedRunSet1Running,
            "RECORDID1",
            OffsetDateTime.now(),
            RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null);
    Run run2 =
        new Run(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            returnedRunSet1Running,
            "RECORDID2",
            OffsetDateTime.now(),
            RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null);

    AbortRequestDetails abortResults = new AbortRequestDetails();
    abortResults.setFailedIds(List.of());
    abortResults.setSubmittedIds(List.of(run1.runId(), run2.runId()));
    when(abortManager.abortRunSet(returnedRunSet1Running.runSetId())).thenReturn(abortResults);

    MvcResult result =
        mockMvc
            .perform(
                post(API_ABORT).param("run_set_id", returnedRunSet1Running.runSetId().toString()))
            .andExpect(status().isOk())
            .andReturn();

    AbortRunSetResponse parsedResponse =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), AbortRunSetResponse.class);

    assertEquals(2, parsedResponse.getRuns().size());
    assertEquals(returnedRunSet1Running.runSetId(), parsedResponse.getRunSetId());
    assertNull(parsedResponse.getErrors());
    assertEquals(CANCELING.toString(), parsedResponse.getState().toString());
  }

  @Test
  void oneFailedOneSucceededRun() throws Exception {
    RunSet returnedRunSet1Running =
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
            false,
            CbasRunSetStatus.RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            2,
            0,
            "inputdefinition",
            "outputDefinition",
            "FOO",
            mockUser.getUserSubjectId());

    Run run1 =
        new Run(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            returnedRunSet1Running,
            "RECORDID1",
            OffsetDateTime.now(),
            RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null);
    Run run2 =
        new Run(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            returnedRunSet1Running,
            "RECORDID2",
            OffsetDateTime.now(),
            RUNNING,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null);

    AbortRequestDetails abortResults = new AbortRequestDetails();
    abortResults.setFailedIds(List.of(run2.runId().toString()));
    abortResults.setSubmittedIds(List.of(run1.runId()));
    when(abortManager.abortRunSet(returnedRunSet1Running.runSetId())).thenReturn(abortResults);

    doThrow(
            new cromwell.client.ApiException(
                "Unable to abort workflow %s.".formatted(run2.runId())))
        .when(cromwellService)
        .cancelRun(run2);

    MvcResult result =
        mockMvc
            .perform(
                post(API_ABORT).param("run_set_id", returnedRunSet1Running.runSetId().toString()))
            .andExpect(status().isOk())
            .andReturn();

    AbortRunSetResponse parsedResponse =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), AbortRunSetResponse.class);

    assertFalse(parsedResponse.getErrors().isEmpty());
    assertEquals(
        "Run set canceled with errors. Unable to abort workflow(s): [%s]".formatted(run2.runId()),
        parsedResponse.getErrors());
    assertEquals(1, parsedResponse.getRuns().size());
    assertNotSame(CANCELING, run2.status());
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
                    result.getResolvedException().getMessage()));
  }

  @Test
  void returnErrorForUserWithNoComputeAccessForPostApi() throws Exception {
    final String optionalInputSourceString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String request =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            optionalInputSourceString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    when(samService.hasComputePermission()).thenReturn(false);

    mockMvc
        .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ForbiddenException))
        .andExpect(
            result ->
                assertEquals(
                    "User doesn't have 'compute' permission on 'workspace' resource",
                    result.getResolvedException().getMessage()));
  }

  @Test
  void returnErrorForUserWithNoComputeAccessForAbortApi() throws Exception {
    when(samService.hasComputePermission()).thenReturn(false);

    mockMvc
        .perform(post(API_ABORT).param("run_set_id", UUID.randomUUID().toString()))
        .andExpect(status().isForbidden())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ForbiddenException))
        .andExpect(
            result ->
                assertEquals(
                    "User doesn't have 'compute' permission on 'workspace' resource",
                    result.getResolvedException().getMessage()));
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
  void returnErrorForPostRequestWithoutToken() throws Exception {
    final String optionalInputSourceString = "{ \"type\" : \"none\", \"record_attribute\" : null }";
    String request =
        requestTemplate.formatted(
            methodVersionId,
            isCallCachingEnabled,
            optionalInputSourceString,
            outputDefinitionAsString,
            recordType,
            "[ \"%s\", \"%s\", \"%s\" ]".formatted(recordId1, recordId2, recordId3));

    when(samService.hasComputePermission())
        .thenThrow(
            new BeanCreationException(
                "BearerToken bean instantiation failed.",
                new UnauthorizedException("Authorization header missing")));

    MvcResult response =
        mockMvc
            .perform(post(API).content(request).contentType(MediaType.APPLICATION_JSON))
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
  void returnErrorForAbortRequestWithoutToken() throws Exception {
    when(samService.hasComputePermission())
        .thenThrow(
            new BeanCreationException(
                "BearerToken bean instantiation failed.",
                new UnauthorizedException("Authorization header missing")));

    MvcResult response =
        mockMvc
            .perform(post(API_ABORT).param("run_set_id", UUID.randomUUID().toString()))
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
    when(samService.hasComputePermission())
        .thenThrow(new SamInterruptedException("InterruptedException thrown for testing purposes"));

    MvcResult response =
        mockMvc
            .perform(post(API_ABORT).param("run_set_id", UUID.randomUUID().toString()))
            .andExpect(status().isInternalServerError())
            .andReturn();

    // verify that the response object is of type ErrorReport and that the exception message is set
    // properly
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(500, errorResponse.getStatusCode());
    assertEquals("InterruptedException thrown for testing purposes", errorResponse.getMessage());
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

  private final WorkflowOutputDefinition recordUpdatingOutputDefinition =
      new WorkflowOutputDefinition()
          .destination(new OutputDestination().type(OutputDestination.TypeEnum.RECORD_UPDATE));

  private final WorkflowOutputDefinition noDestinationOutputDefinition =
      new WorkflowOutputDefinition()
          .destination(new OutputDestination().type(OutputDestination.TypeEnum.NONE));

  @Test
  void testRequestInputsGreaterThanMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    request.setWorkflowInputDefinitions(
        List.of(new WorkflowInputDefinition(), new WorkflowInputDefinition()));
    request.setWorkflowOutputDefinitions(List.of());

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
        List.of(recordUpdatingOutputDefinition, recordUpdatingOutputDefinition));

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
  void testUnusedRequestOutputsDontCountTowardsMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    request.setWdsRecords(new WdsRecordSet().recordIds(Arrays.asList("r1", "r2")));
    request.setWorkflowInputDefinitions(
        List.of(new WorkflowInputDefinition(), new WorkflowInputDefinition()));
    request.setWorkflowOutputDefinitions(
        List.of(
            recordUpdatingOutputDefinition,
            noDestinationOutputDefinition,
            noDestinationOutputDefinition,
            noDestinationOutputDefinition));

    config.setMaxWorkflowInputs(5);
    config.setMaxWorkflowOutputs(1);
    config.setRunSetsMaximumRecordIds(2);

    System.out.println(RunSetsApiController.validateRequestRecordIds(request, config));
    assertTrue(RunSetsApiController.validateRequestRecordIds(request, config).isEmpty());
  }

  @Test
  void testRequestInputsAndOutputsGreaterThanMax() {
    final CbasApiConfiguration config = new CbasApiConfiguration();
    final RunSetRequest request = new RunSetRequest();
    request.setWorkflowInputDefinitions(
        List.of(new WorkflowInputDefinition(), new WorkflowInputDefinition()));
    request.setWorkflowOutputDefinitions(
        List.of(recordUpdatingOutputDefinition, recordUpdatingOutputDefinition));

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
        List.of(recordUpdatingOutputDefinition, recordUpdatingOutputDefinition));

    config.setMaxWorkflowInputs(2);
    config.setMaxWorkflowOutputs(2);

    List<String> actualErrorList =
        RunSetsApiController.validateRequestInputsAndOutputs(request, config);
    assertTrue(actualErrorList.isEmpty());
  }
}
