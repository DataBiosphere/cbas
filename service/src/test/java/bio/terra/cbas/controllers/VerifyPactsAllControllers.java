package bio.terra.cbas.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.sam.SamClient;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager.AbortRequestDetails;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import bio.terra.cbas.runsets.monitoring.SmartRunsPoller;
import bio.terra.cbas.runsets.results.RunCompletionHandler;
import bio.terra.cbas.runsets.results.RunCompletionResult;
import bio.terra.cbas.util.UuidSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.WorkflowDescription;
import cromwell.client.model.WorkflowIdAndStatus;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(
    classes = {
      RunSetsApiController.class,
      RunsApiController.class,
      MethodsApiController.class,
      CbasApiConfiguration.class,
      GlobalExceptionHandler.class
    })
@TestPropertySource(properties = "cbas.cbas-api.runSetsMaximumRecordIds=100")
@TestPropertySource(properties = "cbas.cbas-api.maxWorkflowInputs=100")
@TestPropertySource(properties = "cbas.cbas-api.maxWorkflowOutputs=40")
@TestPropertySource(properties = "cbas.cbas-api.maxWorkflowsInBatch=100")
@Provider("cbas")
@PactBroker()
class VerifyPactsAllControllers {
  private static final String API = "/api/batch/v1/run_sets";

  @MockBean private SamService samService;
  @MockBean private SamClient samClient;
  @MockBean private UsersApi userApi;
  @MockBean private CromwellService cromwellService;
  @MockBean private DockstoreService dockstoreService;
  @MockBean private WdsService wdsService;
  @MockBean private MethodDao methodDao;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private RunSetDao runSetDao;
  @MockBean private RunDao runDao;
  @MockBean private SmartRunSetsPoller smartRunSetsPoller;
  @MockBean private SmartRunsPoller smartRunsPoller;
  @MockBean private UuidSource uuidSource;
  @MockBean private RunSetAbortManager abortManager;
  @MockBean private RunCompletionHandler runCompletionHandler;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private MicrometerMetrics micrometerMetrics;
  @MockBean private CbasContextConfiguration cbasContextConfiguration;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  // mock objects
  UUID fixedMethodVersionUUID = UUID.fromString("90000000-0000-0000-0000-000000000009");
  UUID fixedMethodUUID = UUID.fromString("00000000-0000-0000-0000-000000000009");
  UUID fixedLastRunSetUUIDForMethod = UUID.fromString("0e811493-6013-4fe7-b0eb-f275acdd3c92");
  UUID workspaceId = UUID.randomUUID();

  Method fixedMethod =
      new Method(
          fixedMethodUUID,
          "scATAC-imported-4",
          "scATAC-imported-4 description",
          OffsetDateTime.now(),
          fixedMethodVersionUUID,
          PostMethodRequest.MethodSourceEnum.GITHUB.toString(),
          workspaceId);

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MockMvcTestTarget(mockMvc));
  }

  @State({"user has read permission"})
  public void setReadPermission() throws Exception {
    when(samService.hasReadPermission()).thenReturn(true);
  }

  @State({"user has write permission"})
  public void setWritePermission() throws Exception {
    when(samService.hasWritePermission()).thenReturn(true);
  }

  @State({"user has compute permission"})
  public void setComputePermission() throws Exception {
    when(samService.hasComputePermission()).thenReturn(true);
  }

  @State({"ready to fetch recordId FOO1 from recordType FOO from wdsService"})
  public void initializeFooDataTable() throws Exception {

    // Arrange WDS
    RecordResponse myRecordResponse = new RecordResponse();
    myRecordResponse.setId("FOO1");
    myRecordResponse.setType("FOO");
    RecordAttributes myRecordAttributes = new RecordAttributes();
    myRecordAttributes.put("foo_rating", 10);
    myRecordAttributes.put("bar_string", "this is my bar_string");
    myRecordResponse.setAttributes(myRecordAttributes);
    when(wdsService.getRecord(any(), any())).thenReturn(myRecordResponse);
  }

  @State({"ready to fetch myMethodVersion with UUID 90000000-0000-0000-0000-000000000009"})
  public void initializeDAO() throws Exception {
    // Arrange methodVersion
    MethodVersion myMethodVersion =
        new MethodVersion(
            fixedMethodVersionUUID,
            fixedMethod,
            "imported-version-4",
            "imported-version-4 description",
            OffsetDateTime.now(),
            fixedLastRunSetUUIDForMethod,
            "https://github.com/broadinstitute/warp/blob/develop/pipelines/skylab/scATAC/scATAC.wdl",
            workspaceId);

    // Arrange DAO responses
    when(methodVersionDao.getMethodVersion(any())).thenReturn(myMethodVersion);
    when(runSetDao.createRunSet(any())).thenReturn(1);
    when(methodDao.updateLastRunWithRunSet(any())).thenReturn(1);
    when(methodVersionDao.updateLastRunWithRunSet(any())).thenReturn(1);
    when(runSetDao.updateStateAndRunDetails(any(), any(), any(), any(), any())).thenReturn(1);
    when(runDao.createRun(any())).thenReturn(1);

    // for POST /method endpoint
    when(methodDao.createMethod(any())).thenReturn(1);
    when(methodVersionDao.createMethodVersion(any())).thenReturn(1);
  }

  @State({"cromwell initialized"})
  public void initializeCromwell() throws Exception {
    WorkflowDescription workflowDescription = new WorkflowDescription();
    workflowDescription.valid(true);
    when(cromwellService.describeWorkflow(any())).thenReturn(workflowDescription);
  }

  @State({"ready to receive exactly 1 call to POST run_sets"})
  public HashMap<String, String> initializeOneRunSet() throws Exception {
    String fixedRunSetUUID = "11111111-1111-1111-1111-111111111111";
    String fixedRunUUID = "22222222-2222-2222-2222-222222222222";
    String fixedCromwellRunUUID = "33333333-3333-3333-3333-333333333333";
    when(uuidSource.generateUUID())
        .thenReturn(UUID.fromString(fixedRunSetUUID))
        .thenReturn(UUID.fromString(fixedCromwellRunUUID))
        .thenReturn(UUID.fromString(fixedRunUUID));

    when(cromwellService.submitWorkflowBatch(any(), any(), any()))
        .thenReturn(List.of(new WorkflowIdAndStatus().id(fixedCromwellRunUUID)));
    when(samService.getSamUser())
        .thenReturn(
            new UserStatusInfo().userEmail("foo-email").userSubjectId("bar-id").enabled(true));

    // These values are returned so that they can be injected into variables in the Pact(s)
    HashMap<String, String> providerStateValues = new HashMap<>();
    providerStateValues.put("run_set_id", fixedRunSetUUID);
    providerStateValues.put("run_id", fixedRunUUID);
    return providerStateValues;
  }

  @State({"at least one run set exists with method_id 00000000-0000-0000-0000-000000000009"})
  public void runSetsData() throws Exception {
    UUID methodVersionUUID = UUID.fromString("90000000-0000-0000-0000-000000000009");
    Method myMethod =
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000009"),
            "myMethod name",
            "myMethod description",
            OffsetDateTime.now(),
            methodVersionUUID,
            PostMethodRequest.MethodSourceEnum.GITHUB.toString(),
            workspaceId);

    MethodVersion myMethodVersion =
        new MethodVersion(
            methodVersionUUID,
            myMethod,
            "myMethodVersion name",
            "myMethodVersion description",
            OffsetDateTime.now(),
            UUID.randomUUID(),
            "https://raw.githubusercontent.com/broadinstitute/warp/develop/pipelines/skylab/scATAC/scATAC.wdl",
            workspaceId);

    RunSet targetRunSet =
        new RunSet(
            UUID.randomUUID(),
            myMethodVersion,
            "a run set with methodVersion",
            "a run set with error status",
            false,
            false,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            1,
            1,
            "my input definition string",
            "my output definition string",
            "myRecordType",
            "user-foo",
            workspaceId);

    List<RunSet> response = List.of(targetRunSet);

    when(runSetDao.getRunSetWithMethodId(
            eq(UUID.fromString("00000000-0000-0000-0000-000000000009"))))
        .thenReturn(targetRunSet);

    when(smartRunSetsPoller.updateRunSets(response))
        .thenReturn(new TimeLimitedUpdater.UpdateResult<>(response, 1, 1, true));
  }

  @State({"a run set with UUID 20000000-0000-0000-0000-000000000002 exists"})
  public void postAbort() throws Exception {
    UUID runSetId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    UUID runId = UUID.fromString("30000000-0000-0000-0000-000000000003");
    Run runToBeCancelled = createRunToBeUpdated(runSetId, runId, UUID.randomUUID());

    AbortRequestDetails abortDetails = new AbortRequestDetails();
    abortDetails.setFailedIds(List.of());
    abortDetails.setSubmittedIds(List.of(runToBeCancelled.runId()));

    when(runSetDao.getRunSet(runSetId)).thenReturn(runToBeCancelled.runSet());
    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, any())))
        .thenReturn(Collections.singletonList(runToBeCancelled));

    when(abortManager.abortRunSet(runSetId)).thenReturn(abortDetails);
  }

  @State({"post completed workflow results"})
  public void postCompletedWorkflowResults() throws Exception {
    String fixedRunSetUUID = "11111111-1111-1111-1111-111111111111";
    String fixedRunUUID = "22222222-2222-2222-2222-222222222222";
    String fixedCromwellRunUUID = "12345678-1234-1234-1111-111111111111";

    Run runToBeUpdated =
        createRunToBeUpdated(
            UUID.fromString(fixedRunSetUUID),
            UUID.fromString(fixedRunUUID),
            UUID.fromString(fixedCromwellRunUUID));
    UserStatusInfo userStatusInfo =
        new UserStatusInfo().userEmail("foo-email").userSubjectId("bar-id").enabled(true);
    when(samService.getSamUser()).thenReturn(userStatusInfo);

    when(samClient.checkAuthAccessWithSam()).thenReturn(true);
    when(samService.hasWritePermission()).thenReturn(true);

    when(runDao.getRuns(new RunDao.RunsFilters(null, null, fixedCromwellRunUUID)))
        .thenReturn(Collections.singletonList(runToBeUpdated));
    when(runCompletionHandler.updateResults(
            eq(runToBeUpdated), eq(CbasRunStatus.COMPLETE), any(), eq(Collections.EMPTY_LIST)))
        .thenReturn(RunCompletionResult.SUCCESS);
  }

  private Run createRunToBeUpdated(UUID runSetId, UUID runId, UUID workflowId) {
    RunSet runSetToBeUpdated =
        new RunSet(
            runSetId,
            null,
            null,
            null,
            null,
            null,
            CbasRunSetStatus.RUNNING,
            null,
            null,
            null,
            1,
            null,
            null,
            null,
            null,
            null,
            workspaceId);

    return new Run(
        runId,
        workflowId.toString(),
        runSetToBeUpdated,
        null,
        null,
        CbasRunStatus.RUNNING,
        null,
        null,
        null);
  }
}
