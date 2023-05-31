//package bio.terra.cbas.controllers;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.eq;
//import static org.mockito.Mockito.when;
//
//import au.com.dius.pact.provider.junit5.PactVerificationContext;
//import au.com.dius.pact.provider.junitsupport.Provider;
//import au.com.dius.pact.provider.junitsupport.State;
//import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
//import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
//import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
//import bio.terra.cbas.config.CbasApiConfiguration;
//import bio.terra.cbas.dao.MethodDao;
//import bio.terra.cbas.dao.MethodVersionDao;
//import bio.terra.cbas.dao.RunDao;
//import bio.terra.cbas.dao.RunSetDao;
//import bio.terra.cbas.dependencies.wds.WdsService;
//import bio.terra.cbas.dependencies.wes.CromwellService;
//import bio.terra.cbas.models.CbasRunSetStatus;
//import bio.terra.cbas.models.CbasRunStatus;
//import bio.terra.cbas.models.Method;
//import bio.terra.cbas.models.MethodVersion;
//import bio.terra.cbas.models.Run;
//import bio.terra.cbas.models.RunSet;
//import bio.terra.cbas.monitoring.TimeLimitedUpdater;
//import bio.terra.cbas.runsets.monitoring.RunSetAbortManager;
//import bio.terra.cbas.runsets.monitoring.RunSetAbortManager.AbortRequestDetails;
//import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
//import bio.terra.cbas.util.UuidSource;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import cromwell.client.model.RunId;
//import java.time.OffsetDateTime;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.UUID;
//import org.databiosphere.workspacedata.model.RecordAttributes;
//import org.databiosphere.workspacedata.model.RecordResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.TestTemplate;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//
//@WebMvcTest
//@ContextConfiguration(classes = {RunSetsApiController.class, CbasApiConfiguration.class})
//@Provider("cbas")
//@PactBroker()
//class VerifyPactsRunSetsApiController {
//  private static final String API = "/api/batch/v1/run_sets";
//
//  @MockBean private CromwellService cromwellService;
//  @MockBean private WdsService wdsService;
//  @MockBean private MethodDao methodDao;
//  @MockBean private MethodVersionDao methodVersionDao;
//  @MockBean private RunSetDao runSetDao;
//  @MockBean private RunDao runDao;
//  @MockBean private SmartRunSetsPoller smartRunSetsPoller;
//  @MockBean private UuidSource uuidSource;
//  @MockBean private RunSetAbortManager abortManager;
//  @Autowired private ObjectMapper objectMapper;
//
//  // This mockMVC is what we use to test API requests and responses:
//  @Autowired private MockMvc mockMvc;
//
//  @TestTemplate
//  @ExtendWith(PactVerificationSpringProvider.class)
//  void pactVerificationTestTemplate(PactVerificationContext context) {
//    context.verifyInteraction();
//  }
//
//  @BeforeEach
//  void before(PactVerificationContext context) {
//    context.setTarget(new MockMvcTestTarget(mockMvc));
//  }
//
//  @State({"ready to fetch recordId FOO1 from recordType FOO from wdsService"})
//  public void initializeFooDataTable() throws Exception {
//
//    // Arrange WDS
//    RecordResponse myRecordResponse = new RecordResponse();
//    myRecordResponse.setId("FOO1");
//    myRecordResponse.setType("FOO");
//    RecordAttributes myRecordAttributes = new RecordAttributes();
//    myRecordAttributes.put("foo_rating", 10);
//    myRecordAttributes.put("bar_string", "this is my bar_string");
//    myRecordResponse.setAttributes(myRecordAttributes);
//    when(wdsService.getRecord(any(), any())).thenReturn(myRecordResponse);
//  }
//
//  @State({"ready to fetch myMethodVersion with UUID 90000000-0000-0000-0000-000000000009"})
//  public void initializeDAO() throws Exception {
//    // Arrange methodVersion
//    UUID methodVersionUUID = UUID.fromString("90000000-0000-0000-0000-000000000009");
//    MethodVersion myMethodVersion =
//        new MethodVersion(
//            methodVersionUUID,
//            new Method(
//                UUID.fromString("00000000-0000-0000-0000-000000000009"),
//                "myMethod name",
//                "myMethod description",
//                OffsetDateTime.now(),
//                methodVersionUUID,
//                "myMethod source"),
//            "myMethodVersion name",
//            "myMethodVersion description",
//            OffsetDateTime.now(),
//            UUID.fromString("0e811493-6013-4fe7-b0eb-f275acdd3c92"),
//            "http://myMethodVersionUrl.com");
//    when(methodVersionDao.getMethodVersion(any())).thenReturn(myMethodVersion);
//
//    // Arrange DAO responses
//    when(runSetDao.createRunSet(any())).thenReturn(1);
//    when(methodDao.updateLastRunWithRunSet(any())).thenReturn(1);
//    when(methodVersionDao.updateLastRunWithRunSet(any())).thenReturn(1);
//    when(runSetDao.updateStateAndRunDetails(any(), any(), any(), any(), any())).thenReturn(1);
//    when(runDao.createRun(any())).thenReturn(1);
//  }
//
//  @State({"ready to receive exactly 1 call to POST run_sets"})
//  public HashMap<String, String> initializeOneRunSet() throws Exception {
//    String fixedRunSetUUID = "11111111-1111-1111-1111-111111111111";
//    String fixedRunUUID = "22222222-2222-2222-2222-222222222222";
//    when(uuidSource.generateUUID())
//        .thenReturn(UUID.fromString(fixedRunSetUUID))
//        .thenReturn(UUID.fromString(fixedRunUUID));
//
//    RunId myRunId = new RunId();
//    myRunId.setRunId(fixedRunUUID);
//    when(cromwellService.submitWorkflow(any(), any())).thenReturn(myRunId);
//
//    // These values are returned so that they can be injected into variables in the Pact(s)
//    HashMap<String, String> providerStateValues = new HashMap();
//    providerStateValues.put("run_set_id", fixedRunSetUUID);
//    providerStateValues.put("run_id", fixedRunUUID);
//    return providerStateValues;
//  }
//
//  @State({"at least one run set exists with method_id 00000000-0000-0000-0000-000000000009"})
//  public void runSetsData() throws Exception {
//    UUID methodVersionUUID = UUID.fromString("90000000-0000-0000-0000-000000000009");
//    Method myMethod =
//        new Method(
//            UUID.fromString("00000000-0000-0000-0000-000000000009"),
//            "myMethod name",
//            "myMethod description",
//            OffsetDateTime.now(),
//            methodVersionUUID,
//            "myMethod source");
//
//    MethodVersion myMethodVersion =
//        new MethodVersion(
//            methodVersionUUID,
//            myMethod,
//            "myMethodVersion name",
//            "myMethodVersion description",
//            OffsetDateTime.now(),
//            UUID.randomUUID(),
//            "http://myMethodVersionUrl.com");
//
//    RunSet targetRunSet =
//        new RunSet(
//            UUID.randomUUID(),
//            myMethodVersion,
//            "a run set with methodVersion",
//            "a run set with error status",
//            false,
//            CbasRunSetStatus.COMPLETE,
//            OffsetDateTime.now(),
//            OffsetDateTime.now(),
//            OffsetDateTime.now(),
//            1,
//            1,
//            "my input definition string",
//            "my output definition string",
//            "myRecordType");
//
//    List<RunSet> response = List.of(targetRunSet);
//
//    when(runSetDao.getRunSetWithMethodId(
//            eq(UUID.fromString("00000000-0000-0000-0000-000000000009"))))
//        .thenReturn(targetRunSet);
//
//    when(smartRunSetsPoller.updateRunSets(response))
//        .thenReturn(new TimeLimitedUpdater.UpdateResult<>(response, 1, 1, true));
//  }
//
//  @State({"a run set with UUID 20000000-0000-0000-0000-000000000002 exists"})
//  public void postAbort() throws Exception {
//    UUID runSetId = UUID.fromString("20000000-0000-0000-0000-000000000002");
//    UUID runId = UUID.fromString("30000000-0000-0000-0000-000000000003");
//    RunSet runSetToBeCancelled =
//        new RunSet(
//            runSetId,
//            null,
//            null,
//            null,
//            null,
//            CbasRunSetStatus.RUNNING,
//            null,
//            null,
//            null,
//            1,
//            null,
//            null,
//            null,
//            null);
//
//    Run runToBeCancelled =
//        new Run(
//            runId,
//            UUID.randomUUID().toString(),
//            runSetToBeCancelled,
//            null,
//            null,
//            CbasRunStatus.RUNNING,
//            null,
//            null,
//            null);
//
//    AbortRequestDetails abortDetails = new AbortRequestDetails();
//    abortDetails.setFailedIds(List.of());
//    abortDetails.setSubmittedIds(List.of(runToBeCancelled.runId()));
//
//    when(runSetDao.getRunSet(runSetId)).thenReturn(runSetToBeCancelled);
//    when(runDao.getRuns(new RunDao.RunsFilters(runSetId, any())))
//        .thenReturn(Collections.singletonList(runToBeCancelled));
//
//    when(abortManager.abortRunSet(runSetId)).thenReturn(abortDetails);
//  }
//}
