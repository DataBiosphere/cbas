package bio.terra.cbas.controllers.util;

import static bio.terra.cbas.models.CbasRunStatus.QUEUED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.controllers.GlobalExceptionHandler;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(
    classes = {
      CbasApiConfiguration.class,
      GlobalExceptionHandler.class,
      AsyncExceptionHandler.class
    })
public class TestRunSetHelper {

  @SpyBean AsyncExceptionHandler asyncExceptionHandler;

  private final RunDao runDao = mock(RunDao.class);
  private final RunSetDao runSetDao = mock(RunSetDao.class);
  private final CromwellService cromwellService = mock(CromwellService.class);
  private final WdsService wdsService = mock(WdsService.class);
  private final CbasApiConfiguration cbasApiConfiguration = mock(CbasApiConfiguration.class);
  private final UuidSource uuidSource = mock(UuidSource.class);

  private final RunSetsHelper mockRunSetHelper =
      new RunSetsHelper(
          runDao, runSetDao, cromwellService, wdsService, cbasApiConfiguration, uuidSource);

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

  @Test
  void submissionLaunchesSuccessfully() throws Exception {
    final String recordAttributeValue1 = "hello";
    final String recordAttributeValue2 = "world";
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute1, recordAttributeValue1);
    RecordAttributes recordAttributes2 = new RecordAttributes();
    recordAttributes2.put(recordAttribute1, recordAttributeValue2);

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

    mockRunSetHelper.triggerWorkflowSubmission(
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
    final int recordAttributeValue1 = 100;
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute1, recordAttributeValue1);

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

    mockRunSetHelper.triggerWorkflowSubmission(
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl);

    // verify that both Runs were set to Error state with correct error message
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId1),
            eq(CbasRunStatus.SYSTEM_ERROR),
            any(),
            eq(
                "Error while fetching WDS Records for Record ID(s): {MY_RECORD_ID_2=ApiException thrown for testing purposes.}"));
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId2),
            eq(CbasRunStatus.SYSTEM_ERROR),
            any(),
            eq(
                "Error while fetching WDS Records for Record ID(s): {MY_RECORD_ID_2=ApiException thrown for testing purposes.}"));

    // verify that RunSet was set to Error state
    verify(runSetDao)
        .updateStateAndRunSetDetails(eq(runSetId), eq(CbasRunSetStatus.ERROR), eq(2), eq(2), any());
  }

  @Test
  void inputCoercionErrorDuringSubmission() throws Exception {
    final int recordAttributeValue1 = 100;
    final int recordAttributeValue2 = 200;
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute1, recordAttributeValue1);
    RecordAttributes recordAttributes2 = new RecordAttributes();
    recordAttributes2.put(recordAttribute1, recordAttributeValue2);

    // Set up WDS API responses
    when(wdsService.getRecord(eq(recordType), eq(recordId1), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(eq(recordType), eq(recordId2), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId2).attributes(recordAttributes2));

    when(cbasApiConfiguration.getMaxWorkflowsInBatch()).thenReturn(10);
    when(uuidSource.generateUUID()).thenReturn(UUID.randomUUID()).thenReturn(UUID.randomUUID());

    mockRunSetHelper.triggerWorkflowSubmission(
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl);

    // verify Runs were set to Error state
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId1),
            eq(CbasRunStatus.SYSTEM_ERROR),
            any(),
            eq(
                "Input generation failed for record MY_RECORD_ID_1. Coercion error: Coercion from Integer to String failed for parameter myworkflow.mycall.inputname2. Coercion not supported between these types."));
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId2),
            eq(CbasRunStatus.SYSTEM_ERROR),
            any(),
            eq(
                "Input generation failed for record MY_RECORD_ID_2. Coercion error: Coercion from Integer to String failed for parameter myworkflow.mycall.inputname2. Coercion not supported between these types."));

    // verify that RunSet was set to Error state
    verify(runSetDao)
        .updateStateAndRunSetDetails(eq(runSetId), eq(CbasRunSetStatus.ERROR), eq(2), eq(2), any());
  }

  @Test
  void cromwellApiErrorDuringSubmission() throws Exception {
    final String recordAttributeValue1 = "hello";
    final String recordAttributeValue2 = "world";
    RecordAttributes recordAttributes1 = new RecordAttributes();
    recordAttributes1.put(recordAttribute1, recordAttributeValue1);
    RecordAttributes recordAttributes2 = new RecordAttributes();
    recordAttributes2.put(recordAttribute1, recordAttributeValue2);

    // Set up WDS API responses
    when(wdsService.getRecord(eq(recordType), eq(recordId1), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId1).attributes(recordAttributes1));
    when(wdsService.getRecord(eq(recordType), eq(recordId2), any()))
        .thenReturn(
            new RecordResponse().type(recordType).id(recordId2).attributes(recordAttributes2));

    UUID engineId2 = UUID.randomUUID();
    // throw error when submitting first workflow batch
    when(cromwellService.submitWorkflowBatch(eq(mockWorkflowUrl), any(), any(), any()))
        .thenThrow(
            new cromwell.client.ApiException(
                "ApiException thrown on purpose for testing purposes."))
        .thenReturn(List.of(new WorkflowIdAndStatus().id(engineId2.toString()).status("Running")));

    when(cbasApiConfiguration.getMaxWorkflowsInBatch()).thenReturn(1);
    when(uuidSource.generateUUID()).thenReturn(UUID.randomUUID()).thenReturn(engineId2);

    mockRunSetHelper.triggerWorkflowSubmission(
        runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl);

    // verify that Run 1 was set to Error state
    verify(runDao)
        .updateRunStatusWithError(
            eq(runId1),
            eq(CbasRunStatus.SYSTEM_ERROR),
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
  void asyncSubmissionThrowsException() throws Exception {
    WdsRecordSet wdsRecordSet =
        new WdsRecordSet().recordType(recordType).recordIds(List.of(recordId1, recordId2));

    RunSetRequest runSetRequest =
        new RunSetRequest()
            .runSetName("mock-run-set")
            .methodVersionId(methodVersionId)
            .workflowInputDefinitions(List.of(input1, input2))
            .wdsRecords(wdsRecordSet);

    Map<String, UUID> recordIdToRunIdMapping = new HashMap<>();
    recordIdToRunIdMapping.put(recordId1, runId1);
    recordIdToRunIdMapping.put(recordId2, runId2);

    // throw InterruptedException
    when(wdsService.getRecord(eq(recordType), eq(recordId1), any()))
        .thenThrow(new RuntimeException("thrown for testing purpose"));

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () ->
                mockRunSetHelper.triggerWorkflowSubmission(
                    runSetRequest, runSet, recordIdToRunIdMapping, mockToken, mockWorkflowUrl));

    assertEquals("thrown for testing purpose", exception.getMessage());

    //    verify(asyncExceptionHandler, times(1)).handleUncaughtException(any(), any(), any());
  }
}
