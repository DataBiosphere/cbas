package bio.terra.cbas.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodLastRunDetails;
import bio.terra.cbas.model.MethodListResponse;
import bio.terra.cbas.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest
@ContextConfiguration(classes = MethodsApiController.class)
class TestMethodsApiController {

  private static final String API = "/api/batch/v1/methods";

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private MethodDao methodDao;
  @MockBean private MethodVersionDao methodVersionDao;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  // The object mapper is pulled from the BeanConfig and used to convert to and from JSON in the
  // tests:
  @Autowired private ObjectMapper objectMapper;

  // Set up the database query responses.
  // These answers are always the same, only the business logic that chooses them and interprets
  // the results should change:
  private void initMocks() {
    when(methodDao.getMethods()).thenReturn(List.of(neverRunMethod1, previouslyRunMethod2));
    when(methodDao.getMethod(neverRunMethod1.methodId())).thenReturn(neverRunMethod1);
    when(methodDao.getMethod(previouslyRunMethod2.methodId())).thenReturn(previouslyRunMethod2);

    when(methodVersionDao.getMethodVersionsForMethod(neverRunMethod1))
        .thenReturn(List.of(method1Version1, method1Version2));
    when(methodVersionDao.getMethodVersionsForMethod(previouslyRunMethod2))
        .thenReturn(List.of(method2Version1, method2Version2));

    when(methodVersionDao.getMethodVersion(method1Version1.methodVersionId()))
        .thenReturn(method1Version1);
    when(methodVersionDao.getMethodVersion(method1Version2.methodVersionId()))
        .thenReturn(method1Version2);
    when(methodVersionDao.getMethodVersion(method2Version1.methodVersionId()))
        .thenReturn(method2Version1);
    when(methodVersionDao.getMethodVersion(method2Version2.methodVersionId()))
        .thenReturn(method2Version2);

    when(methodDao.methodLastRunDetailsFromRunSetIds(Set.of(method2RunSet1Id)))
        .thenReturn(Map.of(method2RunSet1Id, method2Version1RunsetDetails));
    when(methodDao.methodLastRunDetailsFromRunSetIds(Set.of(method2RunSet2Id)))
        .thenReturn(Map.of(method2RunSet2Id, method2Version2RunsetDetails));
    when(methodDao.methodLastRunDetailsFromRunSetIds(Set.of(method2RunSet1Id, method2RunSet2Id)))
        .thenReturn(
            Map.of(
                method2RunSet1Id, method2Version1RunsetDetails,
                method2RunSet2Id, method2Version2RunsetDetails));
  }

  @Test
  void returnAllMethodsAndVersions() throws Exception {
    initMocks();
    MvcResult result = mockMvc.perform(get(API)).andExpect(status().isOk()).andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(2, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod1 = parsedResponse.getMethods().get(0);
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(1);

    assertEquals(2, actualResponseForMethod1.getMethodVersions().size());
    assertEquals(2, actualResponseForMethod2.getMethodVersions().size());

    assertFalse(actualResponseForMethod1.getLastRun().isPreviouslyRun());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());
  }

  @Test
  void returnAllMethodsWithoutVersions() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(get(API).param("show_versions", "false"))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(2, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod1 = parsedResponse.getMethods().get(0);
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(1);

    assertNull(actualResponseForMethod1.getMethodVersions());
    assertNull(actualResponseForMethod2.getMethodVersions());

    assertFalse(actualResponseForMethod1.getLastRun().isPreviouslyRun());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());
  }

  @Test
  void returnSpecificMethod() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(get(API).param("method_id", previouslyRunMethod2.methodId().toString()))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(1, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(0);

    assertEquals(previouslyRunMethod2.methodId(), actualResponseForMethod2.getMethodId());
    assertEquals(2, actualResponseForMethod2.getMethodVersions().size());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());
  }

  @Test
  void returnSpecificMethodWithoutVersion() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(
                get(API)
                    .param("method_id", previouslyRunMethod2.methodId().toString())
                    .param("show_versions", "false"))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(1, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(0);

    assertEquals(previouslyRunMethod2.methodId(), actualResponseForMethod2.getMethodId());
    assertNull(actualResponseForMethod2.getMethodVersions());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());
  }

  @Test
  void returnSpecificMethodVersion() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(
                get(API).param("method_version_id", method2Version1.methodVersionId().toString()))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(1, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(0);

    assertEquals(previouslyRunMethod2.methodId(), actualResponseForMethod2.getMethodId());
    assertEquals(1, actualResponseForMethod2.getMethodVersions().size());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());

    var actualVersionDetails = actualResponseForMethod2.getMethodVersions().get(0);
    assertEquals(method2Version1.description(), actualVersionDetails.getDescription());
    assertTrue(actualVersionDetails.getLastRun().isPreviouslyRun());
    assertEquals(method2Version1Runset.runSetId(), actualVersionDetails.getLastRun().getRunSetId());
  }

  private static final Method neverRunMethod1 =
      new Method(
          UUID.randomUUID(),
          "method1",
          "method one",
          OffsetDateTime.now(),
          null,
          "method 1 source");

  private static final MethodVersion method1Version1 =
      new MethodVersion(
          UUID.randomUUID(),
          neverRunMethod1,
          "v1",
          "method one version 1",
          OffsetDateTime.now(),
          null,
          "file://method1/v1.wdl");

  private static final MethodVersion method1Version2 =
      new MethodVersion(
          UUID.randomUUID(),
          neverRunMethod1,
          "v2",
          "method one version 2",
          OffsetDateTime.now(),
          null,
          "file://method1/v2.wdl");

  private static final UUID method2RunSet1Id = UUID.randomUUID();
  private static final UUID method2RunSet2Id = UUID.randomUUID();

  private static final Method previouslyRunMethod2 =
      new Method(
          UUID.randomUUID(),
          "method2",
          "method two",
          OffsetDateTime.now(),
          method2RunSet2Id,
          "method 2 source");

  private static final UUID method2Version1VersionID = UUID.randomUUID();
  private static final MethodVersion method2Version1 =
      new MethodVersion(
          method2Version1VersionID,
          previouslyRunMethod2,
          "v1",
          "method two version 1",
          OffsetDateTime.now(),
          method2RunSet1Id,
          "file://method2/v1.wdl");

  private static final UUID method2Version2VersionID = UUID.randomUUID();
  private static final MethodVersion method2Version2 =
      new MethodVersion(
          method2Version2VersionID,
          previouslyRunMethod2,
          "v2",
          "method two version 2",
          OffsetDateTime.now(),
          method2RunSet2Id,
          "file://method2/v2.wdl");

  private static final RunSet method2Version1Runset =
      new RunSet(
          method2RunSet1Id,
          method2Version1,
          "Run workflow 2 v1",
          "Run workflow 2 v1",
          false,
          CbasRunSetStatus.COMPLETE,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          100,
          0,
          "[]",
          "[]",
          "FOO");

  private static final MethodLastRunDetails method2Version1RunsetDetails =
      new MethodLastRunDetails()
          .previouslyRun(true)
          .timestamp(DateUtils.convertToDate(OffsetDateTime.now()))
          .methodVersionName("method two version 1")
          .methodVersionId(method2Version1VersionID)
          .runSetId(method2RunSet1Id);

  private static final RunSet method2Version2Runset =
      new RunSet(
          method2RunSet2Id,
          method2Version2,
          "Run workflow 2 v1, take 2",
          "Run workflow 2 v1 a second time",
          false,
          CbasRunSetStatus.COMPLETE,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          100,
          0,
          "[]",
          "[]",
          "FOO");

  private static final MethodLastRunDetails method2Version2RunsetDetails =
      new MethodLastRunDetails()
          .previouslyRun(true)
          .timestamp(DateUtils.convertToDate(method2Version2Runset.submissionTimestamp()))
          .methodVersionName(method2Version2Runset.methodVersion().name())
          .methodVersionId(method2Version2Runset.getMethodVersionId())
          .runSetId(method2Version2Runset.runSetId());
}
