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
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import bio.terra.cbas.util.UuidSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.RunId;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {RunSetsApiController.class, CbasApiConfiguration.class})
@Provider("cbas")
@PactBroker()
class VerifyPactsRunSetsApiController {
  private static final String API = "/api/batch/v1/run_sets";

  @MockBean private CromwellService cromwellService;
  @MockBean private WdsService wdsService;
  @MockBean private MethodDao methodDao;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private RunSetDao runSetDao;
  @MockBean private RunDao runDao;
  @MockBean private SmartRunSetsPoller smartRunSetsPoller;
  @MockBean private UuidSource uuidSource;
  @Autowired private ObjectMapper objectMapper;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MockMvcTestTarget(mockMvc));
  }

  // TODO: rename this state and this function after basic functionality is implemented
  @State({"post run sets"})
  public HashMap<String, String> postRunSets() throws Exception {
    System.out.println("####### POST RUN_SETS STATE FUNCTION HAS BEEN CALLED #######");
    UUID methodVersionUUID = UUID.fromString("90000000-0000-0000-0000-000000000009");

    RecordResponse myRecordResponse = new RecordResponse();
    myRecordResponse.setId("FOO1");
    myRecordResponse.setType("FOO");

    RecordAttributes myRecordAttributes = new RecordAttributes();
    myRecordAttributes.put("foo_rating", 10);
    myRecordAttributes.put("bar_string", "this is my bar_string");

    myRecordResponse.setAttributes(myRecordAttributes);

    when(wdsService.getRecord(any(), any())).thenReturn(myRecordResponse);

    Method myMethod =
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000009"),
            "myMethod name",
            "myMethod description",
            OffsetDateTime.now(),
            methodVersionUUID,
            "myMethod source");

    UUID fixedRandomUUID = UUID.fromString("01234567-8910-1112-1314-151617181920");

    when(uuidSource.randomUUID()).thenReturn(fixedRandomUUID);

    MethodVersion myMethodVersion =
        new MethodVersion(
            methodVersionUUID,
            myMethod,
            "myMethodVersion name",
            "myMethodVersion description",
            OffsetDateTime.now(),
            UUID.fromString("0e811493-6013-4fe7-b0eb-f275acdd3c92"),
            "http://myMethodVersionUrl.com");

    when(methodVersionDao.getMethodVersion(any())).thenReturn(myMethodVersion);

    RunSet targetRunSet =
        new RunSet(
            fixedRandomUUID,
            myMethodVersion,
            "a run set with methodVersion",
            "a run set with error status",
            false,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            1,
            1,
            "my input definition string",
            "my output definition string",
            "myRecordType");

    // TODO: is this the right return integer?
    // TODO: is it ok to set the function argument matcher to any()?
    when(runSetDao.createRunSet(any())).thenReturn(1);
    when(methodDao.updateLastRunWithRunSet(any())).thenReturn(1);
    when(methodVersionDao.updateLastRunWithRunSet(any())).thenReturn(1);
    when(runSetDao.updateStateAndRunDetails(any(), any(), any(), any(), any())).thenReturn(1);

    RunId myRunId = new RunId();
    myRunId.setRunId("myRunId_UUID");
    when(cromwellService.submitWorkflow(any(), any())).thenReturn(myRunId);

    HashMap<String, String> providerStateValues = new HashMap();
    providerStateValues.put("run_set_id", fixedRandomUUID.toString());
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
            "myMethod source");

    MethodVersion myMethodVersion =
        new MethodVersion(
            methodVersionUUID,
            myMethod,
            "myMethodVersion name",
            "myMethodVersion description",
            OffsetDateTime.now(),
            UUID.randomUUID(),
            "http://myMethodVersionUrl.com");

    RunSet targetRunSet =
        new RunSet(
            UUID.randomUUID(),
            myMethodVersion,
            "a run set with methodVersion",
            "a run set with error status",
            false,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            1,
            1,
            "my input definition string",
            "my output definition string",
            "myRecordType");

    List<RunSet> response = List.of(targetRunSet);

    when(runSetDao.getRunSetWithMethodId(
            eq(UUID.fromString("00000000-0000-0000-0000-000000000009"))))
        .thenReturn(targetRunSet);

    when(smartRunSetsPoller.updateRunSets(response))
        .thenReturn(new TimeLimitedUpdater.UpdateResult<>(response, 1, 1, true));
  }
}
