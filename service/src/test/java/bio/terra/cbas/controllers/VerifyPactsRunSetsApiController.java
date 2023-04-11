package bio.terra.cbas.controllers;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
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

  @State({"at least one run set exists with method_id 00000000-0000-0000-0000-000000000009"})
  public void runSetsData() throws Exception {
    Method myMethod =
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000009"),
            "myMethod name",
            "myMethod description",
            OffsetDateTime.now(),
            UUID.fromString("90000000-0000-0000-0000-000000000009"),
            "myMethod source");

    MethodVersion myMethodVersion =
        new MethodVersion(
            UUID.randomUUID(),
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
