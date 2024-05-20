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
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunCreationException;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunSetCreationException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.controllers.GlobalExceptionHandler;
import bio.terra.cbas.controllers.util.AsyncExceptionHandler;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionLiteralValue;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.model.WdsRecordSet;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(
    classes = {
      CbasApiConfiguration.class,
      GlobalExceptionHandler.class,
      AsyncExceptionHandler.class
    })
class TestRunSetsService {

  private final RunDao runDao = mock(RunDao.class);
  private final RunSetDao runSetDao = mock(RunSetDao.class);
  private final MethodDao methodDao = mock(MethodDao.class);
  private final MethodVersionDao methodVersionDao = mock(MethodVersionDao.class);
  private final CromwellService cromwellService = mock(CromwellService.class);
  private final WdsService wdsService = mock(WdsService.class);
  private final CbasApiConfiguration cbasApiConfiguration = mock(CbasApiConfiguration.class);
  private final UuidSource uuidSource = mock(UuidSource.class);
  private final ObjectMapper objectMapper = mock(ObjectMapper.class);
  private final CbasContextConfiguration cbasContextConfiguration =
      mock(CbasContextConfiguration.class);

  private final RunSetsService mockRunSetsService =
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
          cbasContextConfiguration);

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
                  workspaceId),
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

  private final MethodVersion methodVersion =
      new MethodVersion(
          methodVersionId,
          new Method(
              methodId,
              "methodname",
              "methoddescription",
              OffsetDateTime.now(),
              UUID.randomUUID(),
              "GitHub",
              workspaceId),
          "version name",
          "version description",
          OffsetDateTime.now(),
          null,
          "https://abc.com/path-to-wdl",
          workspaceId,
          "test_branch",
          Optional.empty());

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
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl);

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
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl);

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
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl);

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
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl);

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
}
