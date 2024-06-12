package bio.terra.cbas.service;

import static bio.terra.cbas.models.CbasRunStatus.QUEUED;
import static bio.terra.cbas.models.CbasRunStatus.SYSTEM_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunCreationException;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunSetCreationException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.bard.BardService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.model.WdsRecordSet;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.util.UuidSource;
import bio.terra.common.iam.BearerToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.WorkflowIdAndStatus;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestRunSetsService {

  private final UUID methodId = UUID.randomUUID();
  private final UUID methodVersionId = UUID.randomUUID();
  private final UUID runSetId = UUID.randomUUID();
  private final UUID workspaceId = UUID.randomUUID();
  private final UUID runId1 = UUID.randomUUID();
  private final UUID runId2 = UUID.randomUUID();
  private final UUID engineId1 = UUID.randomUUID();
  private final UUID engineId2 = UUID.randomUUID();

  private final String recordId1 = "MY_RECORD_ID_1";
  private final String recordId2 = "MY_RECORD_ID_2";
  private final String recordType = "FOO";
  private final String recordAttribute1 = "MY_RECORD_ATTRIBUTE";

  private final RecordAttributes recordAttributes1 =
      new RecordAttributes() {
        {
          put(recordAttribute1, "hello");
        }
      };
  private final RecordAttributes recordAttributes2 =
      new RecordAttributes() {
        {
          put(recordAttribute1, "world");
        }
      };

  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);
  private final BearerToken mockToken = new BearerToken("mock-token");

  private final String mockWorkflowUrl = "https://path-to-wdl.com";

  WorkflowInputDefinition input1 =
      new WorkflowInputDefinition()
          .inputName("myworkflow.mycall.inputname1")
          .inputType(
              new ParameterTypeDefinitionPrimitive()
                  .primitiveType(PrimitiveParameterValueType.STRING)
                  .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
          .source(
              new ParameterDefinitionLiteralValue()
                  .parameterValue("literal value")
                  .type(ParameterDefinition.TypeEnum.LITERAL));
  WorkflowInputDefinition input2 =
      new WorkflowInputDefinition()
          .inputName("myworkflow.mycall.inputname2")
          .inputType(
              new ParameterTypeDefinitionPrimitive()
                  .primitiveType(PrimitiveParameterValueType.STRING)
                  .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
          .source(
              new ParameterDefinitionRecordLookup()
                  .recordAttribute("MY_RECORD_ATTRIBUTE")
                  .type(ParameterDefinition.TypeEnum.RECORD_LOOKUP));

  private final RunSet runSet =
      new RunSet(
          runSetId,
          new MethodVersion(
              methodVersionId,
              new Method(
                  methodId,
                  "methodName",
                  "methodDescription",
                  OffsetDateTime.now(),
                  UUID.randomUUID(),
                  "method source",
                  workspaceId,
                  Optional.empty()),
              "version name",
              "version description",
              OffsetDateTime.now(),
              UUID.randomUUID(),
              mockWorkflowUrl,
              workspaceId,
              "0.0.15",
              Optional.empty()),
          "",
          "",
          false,
          false,
          CbasRunSetStatus.QUEUED,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          2,
          0,
          "inputDefinition",
          "outputDefinition",
          recordType,
          mockUser.getUserSubjectId(),
          workspaceId);
  private final Run run1 =
      new Run(
          runId1,
          null,
          runSet,
          recordId1,
          OffsetDateTime.now(),
          QUEUED,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          null);
  private final Run run2 =
      new Run(
          runId2,
          null,
          runSet,
          recordId2,
          OffsetDateTime.now(),
          QUEUED,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          null);

  private final WdsRecordSet wdsRecordSet =
      new WdsRecordSet().recordType(recordType).recordIds(List.of(recordId1, recordId2));

  private final RunSetRequest runSetRequest =
      new RunSetRequest()
          .runSetName("mock-run-set")
          .methodVersionId(methodVersionId)
          .workflowInputDefinitions(List.of(input1, input2))
          .wdsRecords(wdsRecordSet);

  private final Map<String, UUID> recordIdToRunIdMapping =
      new HashMap<>() {
        {
          put(recordId1, runId1);
          put(recordId2, runId2);
        }
      };

  private MethodVersion methodVersion =
      new MethodVersion(
          methodVersionId,
          new Method(
              methodId,
              "methodname",
              "methoddescription",
              OffsetDateTime.now(),
              UUID.randomUUID(),
              "GitHub",
              workspaceId,
              Optional.of(new GithubMethodDetails("repo", "org", "path", false, methodId))),
          "version name",
          "version description",
          OffsetDateTime.now(),
          null,
          "https://abc.com/path-to-wdl",
          workspaceId,
          "test_branch",
          Optional.empty());
  private RunDao runDao;
  private RunSetDao runSetDao;
  private MethodDao methodDao;
  private MethodVersionDao methodVersionDao;
  private CromwellService cromwellService;
  private WdsService wdsService;
  private CbasApiConfiguration cbasApiConfiguration;
  private UuidSource uuidSource;
  private ObjectMapper objectMapper;
  private CbasContextConfiguration cbasContextConfiguration;
  private BardService bardService;
  private RunSetsService mockRunSetsService;

  @BeforeEach
  void init() {
    runDao = mock(RunDao.class);
    runSetDao = mock(RunSetDao.class);
    methodDao = mock(MethodDao.class);
    methodVersionDao = mock(MethodVersionDao.class);
    cromwellService = mock(CromwellService.class);
    wdsService = mock(WdsService.class);
    cbasApiConfiguration = mock(CbasApiConfiguration.class);
    uuidSource = mock(UuidSource.class);
    objectMapper = mock(ObjectMapper.class);
    cbasContextConfiguration = mock(CbasContextConfiguration.class);
    bardService = mock(BardService.class);

    mockRunSetsService =
        new RunSetsService(
            runDao,
            runSetDao,
            methodDao,
            methodVersionDao,
            cromwellService,
            wdsService,
            cbasApiConfiguration,
            uuidSource,
            objectMapper,
            cbasContextConfiguration,
            bardService);
  }

  @Test
  void submissionLaunchesSuccessfully() throws Exception {
    // Set up WDS API responses
    when(wdsService.getRecord(eq(recordType), eq(recordId1), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(eq(recordType), eq(recordId2), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId2).attributes(recordAttributes2));

    when(cromwellService.submitWorkflowBatch(eq(mockWorkflowUrl), any(), any(), any()))
        .thenReturn(
            List.of(
                new WorkflowIdAndStatus().id(engineId1.toString()).status("Running"),
                new WorkflowIdAndStatus().id(engineId2.toString()).status("Running")));

    when(cbasApiConfiguration.getMaxWorkflowsInBatch()).thenReturn(10);
    when(uuidSource.generateUUID()).thenReturn(engineId1).thenReturn(engineId2);

    mockRunSetsService.triggerWorkflowSubmission(
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl, methodVersion);

    // verify that Runs were set to Initializing state
    verify(runDao)
        .updateEngineIdAndRunStatus(
            eq(runId1), eq(engineId1), eq(CbasRunStatus.INITIALIZING), any());
    verify(runDao)
        .updateEngineIdAndRunStatus(
            eq(runId2), eq(engineId2), eq(CbasRunStatus.INITIALIZING), any());

    // verify that RunSet was set to Running state
    verify(runSetDao)
        .updateStateAndRunSetDetails(
            eq(runSetId), eq(CbasRunSetStatus.RUNNING), eq(2), eq(0), any());

    String eventName = "workflow-submission";
    HashMap<String, String> properties =
        mockRunSetsService.getRunSetEventProperties(
            runSetRequest, methodVersion, List.of(engineId1.toString(), engineId2.toString()));
    verify(bardService).logEvent(eventName, properties, mockToken);
  }

  @Test
  void wdsErrorDuringSubmission() throws Exception {
    // Set up WDS API responses
    when(wdsService.getRecord(eq(recordType), eq(recordId1), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    // return error when fetching record data for record ID 2
    when(wdsService.getRecord(eq(recordType), eq(recordId2), any()))
        .thenThrow(
            new WdsServiceApiException(
                new org.databiosphere.workspacedata.client.ApiException(
                    400, "ApiException thrown for testing purposes.")));

    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, List.of(QUEUED))))
        .thenReturn(List.of(run1, run2));

    mockRunSetsService.triggerWorkflowSubmission(
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl, methodVersion);

    // verify that both Runs were set to Error state with correct error message
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId1),
            eq(SYSTEM_ERROR),
            any(),
            eq(
                "Error while fetching WDS Records for Record ID(s): {MY_RECORD_ID_2=ApiException thrown for testing purposes.}"));
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId2),
            eq(SYSTEM_ERROR),
            any(),
            eq(
                "Error while fetching WDS Records for Record ID(s): {MY_RECORD_ID_2=ApiException thrown for testing purposes.}"));

    // verify that RunSet was set to Error state
    verify(runSetDao)
        .updateStateAndRunSetDetails(eq(runSetId), eq(CbasRunSetStatus.ERROR), eq(2), eq(2), any());
    verifyNoInteractions(bardService);
  }

  @Test
  void inputCoercionErrorDuringSubmission() throws Exception {
    final int recordAttributeValueInt1 = 100;
    final int recordAttributeValueInt2 = 200;
    RecordAttributes incorrectRecordAttributes1 = new RecordAttributes();
    incorrectRecordAttributes1.put(recordAttribute1, recordAttributeValueInt1);
    RecordAttributes incorrectRecordAttributes2 = new RecordAttributes();
    incorrectRecordAttributes2.put(recordAttribute1, recordAttributeValueInt2);

    // Set up WDS API responses
    when(wdsService.getRecord(eq(recordType), eq(recordId1), any()))
        .thenReturn(
            new RecordResponse()
                .type(recordType)
                .id(recordId1)
                .attributes(incorrectRecordAttributes1));
    when(wdsService.getRecord(eq(recordType), eq(recordId2), any()))
        .thenReturn(
            new RecordResponse()
                .type(recordType)
                .id(recordId2)
                .attributes(incorrectRecordAttributes2));

    when(cbasApiConfiguration.getMaxWorkflowsInBatch()).thenReturn(10);
    when(uuidSource.generateUUID()).thenReturn(UUID.randomUUID()).thenReturn(UUID.randomUUID());

    mockRunSetsService.triggerWorkflowSubmission(
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl, methodVersion);

    // verify Runs were set to Error state
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId1),
            eq(SYSTEM_ERROR),
            any(),
            eq(
                "Input generation failed for record MY_RECORD_ID_1. Coercion error: Coercion from Integer to String failed for parameter myworkflow.mycall.inputname2. Coercion not supported between these types."));
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId2),
            eq(SYSTEM_ERROR),
            any(),
            eq(
                "Input generation failed for record MY_RECORD_ID_2. Coercion error: Coercion from Integer to String failed for parameter myworkflow.mycall.inputname2. Coercion not supported between these types."));

    // verify that RunSet was set to Error state
    verify(runSetDao)
        .updateStateAndRunSetDetails(eq(runSetId), eq(CbasRunSetStatus.ERROR), eq(2), eq(2), any());
  }

  @Test
  void cromwellApiErrorDuringSubmission() throws Exception {
    // Set up WDS API responses
    when(wdsService.getRecord(eq(recordType), eq(recordId1), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(eq(recordType), eq(recordId2), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId2).attributes(recordAttributes2));

    // throw error when submitting first workflow batch
    when(cromwellService.submitWorkflowBatch(eq(mockWorkflowUrl), any(), any(), any()))
        .thenThrow(
            new cromwell.client.ApiException(
                "ApiException thrown on purpose for testing purposes."))
        .thenReturn(List.of(new WorkflowIdAndStatus().id(engineId2.toString()).status("Running")));

    when(cbasApiConfiguration.getMaxWorkflowsInBatch()).thenReturn(1);
    when(uuidSource.generateUUID()).thenReturn(UUID.randomUUID()).thenReturn(engineId2);

    mockRunSetsService.triggerWorkflowSubmission(
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl, methodVersion);

    // verify that Run 1 was set to Error state
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId1),
            eq(SYSTEM_ERROR),
            any(),
            startsWith(
                "Cromwell submission failed for batch in RunSet %s. ApiException: Message: ApiException thrown on purpose for testing purposes."
                    .formatted(runSetId)));
    // verify that Run 2 was set to Initializing state
    verify(runDao)
        .updateEngineIdAndRunStatus(
            eq(runId2), eq(engineId2), eq(CbasRunStatus.INITIALIZING), any());

    // verify that RunSet was set to Running state with appropriate error count
    verify(runSetDao)
        .updateStateAndRunSetDetails(
            eq(runSetId), eq(CbasRunSetStatus.RUNNING), eq(2), eq(1), any());
  }

  @Test
  void registerRunSetSuccess() throws Exception {
    when(uuidSource.generateUUID()).thenReturn(runSetId);
    when(runSetDao.createRunSet(any())).thenReturn(1);

    RunSet actualRunSet = mockRunSetsService.registerRunSet(runSetRequest, mockUser, methodVersion);

    // verify run set values
    assertEquals(runSetId, actualRunSet.runSetId());
    assertEquals(CbasRunSetStatus.QUEUED, actualRunSet.status());
    assertEquals(0, actualRunSet.runCount());

    // verify method DAO and methodVersion DAO methods were called as expected
    verify(methodDao, atMostOnce()).updateLastRunWithRunSet(actualRunSet);
    verify(methodVersionDao, atMostOnce()).updateLastRunWithRunSet(actualRunSet);
  }

  @Test
  void registerRunSetFailure() {
    when(uuidSource.generateUUID()).thenReturn(UUID.randomUUID());
    when(runSetDao.createRunSet(any())).thenReturn(0);

    RunSetCreationException exception =
        assertThrows(
            RunSetCreationException.class,
            () -> mockRunSetsService.registerRunSet(runSetRequest, mockUser, methodVersion));

    assertEquals("Failed to create new RunSet for 'mock-run-set'.", exception.getMessage());

    // verify method DAO and methodVersion DAO methods weren't called
    verify(methodDao, never()).updateLastRunWithRunSet(any());
    verify(methodVersionDao, never()).updateLastRunWithRunSet(any());
  }

  @Test
  void registerRunsInRunSetSuccess() throws Exception {
    when(runDao.createRun(any())).thenReturn(1).thenReturn(1);

    List<RunStateResponse> actualResponse =
        mockRunSetsService.registerRunsInRunSet(runSet, recordIdToRunIdMapping);

    // verify that Run Set was updated with correct Runs count
    verify(runSetDao)
        .updateStateAndRunSetDetails(
            eq(runSetId), eq(CbasRunSetStatus.QUEUED), eq(2), eq(0), any());

    assertEquals(2, actualResponse.size());
    assertEquals(RunState.QUEUED, actualResponse.get(0).getState());
    assertEquals(RunState.QUEUED, actualResponse.get(1).getState());
  }

  @Test
  void registerRunsInRunSetFailure() {
    when(runDao.createRun(any())).thenReturn(1).thenReturn(0);

    when(runDao.getRuns(any())).thenReturn(List.of(run1));

    RunCreationException exception =
        assertThrows(
            RunCreationException.class,
            () -> mockRunSetsService.registerRunsInRunSet(runSet, recordIdToRunIdMapping));

    assertThat(exception.getMessage(), containsString("Failed to create new Run"));

    // verify that 1 Run that were registered in DB are set to Error state
    verify(runDao).updateRunStatusWithError(eq(run1.runId()), eq(SYSTEM_ERROR), any(), any());

    // verify that RunSet is marked in Error state
    verify(runSetDao)
        .updateStateAndRunSetDetails(any(), eq(CbasRunSetStatus.ERROR), eq(1), eq(1), any());
  }

  @Test
  void testGetRunSetEventPropertiesDockstore() {
    Method dockstoreMethod =
        new Method(
            methodId,
            "methodname",
            "methoddescription",
            OffsetDateTime.now(),
            UUID.randomUUID(),
            PostMethodRequest.MethodSourceEnum.DOCKSTORE.toString(),
            workspaceId,
            Optional.empty());
    MethodVersion dockstoreMethodVersion = methodVersion.withMethod(dockstoreMethod);
    RunSetRequest request =
        new RunSetRequest()
            .runSetName("testRun")
            .methodVersionId(dockstoreMethodVersion.methodVersionId())
            .wdsRecords(new WdsRecordSet().recordIds(List.of("1", "2", "3")));
    List<String> cromwellWorkflowIds = List.of(UUID.randomUUID().toString());
    Map<String, String> expectedProperties =
        getDefaultProperties(request, dockstoreMethodVersion, cromwellWorkflowIds);
    Map<String, String> properties =
        mockRunSetsService.getRunSetEventProperties(
            request, dockstoreMethodVersion, cromwellWorkflowIds);
    assertEquals(expectedProperties, properties);
  }

  @Test
  void testGetRunSetEventPropertiesGitHub() {
    RunSetRequest request =
        new RunSetRequest()
            .runSetName("testRun")
            .methodVersionId(methodVersion.methodVersionId())
            .wdsRecords(new WdsRecordSet().recordIds(List.of("1", "2", "3")));
    List<String> cromwellWorkflowIds = List.of(UUID.randomUUID().toString());
    Map<String, String> expectedProperties =
        getDefaultProperties(request, methodVersion, cromwellWorkflowIds);
    GithubMethodDetails githubMethodDetails = methodVersion.method().githubMethodDetails().get();
    expectedProperties.put("githubOrganization", githubMethodDetails.organization());
    expectedProperties.put("githubRepository", githubMethodDetails.repository());
    expectedProperties.put("githubIsPrivate", githubMethodDetails.isPrivate().toString());
    Map<String, String> properties =
        mockRunSetsService.getRunSetEventProperties(request, methodVersion, cromwellWorkflowIds);
    assertEquals(expectedProperties, properties);
  }

  private HashMap<String, String> getDefaultProperties(
      RunSetRequest request, MethodVersion methodVersion, List<String> cromwellWorkflowIds) {
    HashMap<String, String> properties = new HashMap<>();
    properties.put("runSetName", request.getRunSetName());
    properties.put("methodName", methodVersion.method().name());
    properties.put("methodSource", methodVersion.method().methodSource());
    properties.put("methodVersionName", methodVersion.name());
    properties.put("methodVersionUrl", methodVersion.url());
    properties.put("recordCount", String.valueOf(request.getWdsRecords().getRecordIds().size()));
    properties.put("workflowIds", cromwellWorkflowIds.toString());
    return properties;
  }
}
