package bio.terra.cbas.dependencies.bard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bio.terra.bard.api.DefaultApi;
import bio.terra.bard.client.ApiClient;
import bio.terra.bard.model.EventsEvent200Response;
import bio.terra.bard.model.EventsEventLogRequest;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.config.BardServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.WdsRecordSet;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.common.iam.BearerToken;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class TestBardService {
  private BardServerConfiguration bardServerConfiguration;
  private BardServiceImpl bardServiceImpl;
  private DefaultApi defaultApi;
  private DefaultApi defaultAuthApi;
  private BearerToken userToken;
  private final String appId = "cbas";

  @BeforeEach
  void setup() {
    bardServerConfiguration = mock(BardServerConfiguration.class);
    lenient().when(bardServerConfiguration.enabled()).thenReturn(true);
    BardClient bardClient = mock(BardClient.class);
    bardServiceImpl = new BardServiceImpl(bardClient, bardServerConfiguration);

    // Mock unauthenticated Bard API
    ApiClient apiClient = mock(ApiClient.class);
    lenient().when(bardClient.apiClient()).thenReturn(apiClient);
    defaultApi = mock(DefaultApi.class);
    lenient().when(bardClient.defaultApi(apiClient)).thenReturn(defaultApi);

    // Mock authenticated Bard API
    ApiClient authApiClient = mock(ApiClient.class);
    lenient().when(bardClient.bardAuthClient(any())).thenReturn(authApiClient);
    defaultAuthApi = mock(DefaultApi.class);
    lenient().when(bardClient.defaultApi(authApiClient)).thenReturn(defaultAuthApi);
    userToken = new BearerToken("foo");
  }

  @Test
  void testBardLogRunSetEvent() {
    Method method = getTestMethod(PostMethodRequest.MethodSourceEnum.DOCKSTORE);
    MethodVersion methodVersion = getTestMethodVersion(method);
    RunSetRequest request =
        new RunSetRequest()
            .runSetName("testRun")
            .methodVersionId(methodVersion.methodVersionId())
            .wdsRecords(new WdsRecordSet().recordIds(List.of("1", "2", "3")));
    List<String> cromwellWorkflowIds = List.of(UUID.randomUUID().toString());
    bardServiceImpl.logRunSetEvent(request, methodVersion, cromwellWorkflowIds, userToken);

    Map<String, String> properties =
        getDefaultProperties(request, methodVersion, cromwellWorkflowIds);
    EventsEventLogRequest eventLogRequest = new EventsEventLogRequest().properties(properties);
    verify(defaultAuthApi).eventsEventLog("workflow-submission", appId, eventLogRequest);
  }

  @Test
  void testBardLogGithubRunSetEvent() {
    Method method = getTestMethod(PostMethodRequest.MethodSourceEnum.GITHUB);
    MethodVersion methodVersion = getTestMethodVersion(method);
    RunSetRequest request =
        new RunSetRequest()
            .runSetName("testRun")
            .methodVersionId(methodVersion.methodVersionId())
            .wdsRecords(new WdsRecordSet().recordIds(List.of("1", "2", "3")));
    List<String> cromwellWorkflowIds = List.of(UUID.randomUUID().toString());
    bardServiceImpl.logRunSetEvent(request, methodVersion, cromwellWorkflowIds, userToken);

    GithubMethodDetails githubMethodDetails = method.githubMethodDetails().get();
    Map<String, String> properties =
        getDefaultProperties(request, methodVersion, cromwellWorkflowIds);
    properties.put("githubOrganization", githubMethodDetails.organization());
    properties.put("githubRepository", githubMethodDetails.repository());
    properties.put("githubIsPrivate", githubMethodDetails.isPrivate().toString());
    EventsEventLogRequest eventLogRequest = new EventsEventLogRequest().properties(properties);
    verify(defaultAuthApi).eventsEventLog("workflow-submission", appId, eventLogRequest);
  }

  @Test
  void logRunSetEventDisabled() {
    when(bardServerConfiguration.enabled()).thenReturn(false);
    Method method = getTestMethod(PostMethodRequest.MethodSourceEnum.DOCKSTORE);
    MethodVersion methodVersion = getTestMethodVersion(method);
    RunSetRequest request =
        new RunSetRequest()
            .runSetName("testRun")
            .methodVersionId(methodVersion.methodVersionId())
            .wdsRecords(new WdsRecordSet().recordIds(List.of("1", "2", "3")));
    List<String> cromwellWorkflowIds = List.of(UUID.randomUUID().toString());
    bardServiceImpl.logRunSetEvent(request, methodVersion, cromwellWorkflowIds, userToken);
    verifyNoInteractions(defaultAuthApi);
  }

  @Test
  void testBardLogEventSuccess() {
    EventsEventLogRequest eventLogRequest = new EventsEventLogRequest().properties(Map.of());
    bardServiceImpl.logEvent("testEvent", Map.of(), userToken);
    verify(defaultAuthApi).eventsEventLog("testEvent", appId, eventLogRequest);
  }

  @Test
  void testBardLogEventErrorDoesNotThrow() {
    when(defaultAuthApi.eventsEventLog(any(), any(), any()))
        .thenThrow(new RestClientException("API error"));
    EventsEventLogRequest eventLogRequest = new EventsEventLogRequest().properties(Map.of());
    bardServiceImpl.logEvent("testEvent", Map.of(), userToken);
    verify(defaultAuthApi).eventsEventLog("testEvent", appId, eventLogRequest);
  }

  @Test
  void testBardStatusSuccess() {
    when(defaultApi.systemStatus()).thenReturn(new EventsEvent200Response());
    HealthCheck.Result result = bardServiceImpl.checkHealth();
    assertTrue(result.isOk());
    assertEquals("Ok", result.message());
  }

  @Test
  void testBardStatusFailure() {
    String errorMessage = "API error";
    when(defaultApi.systemStatus()).thenThrow(new RestClientException(errorMessage));
    HealthCheck.Result result = bardServiceImpl.checkHealth();
    assertFalse(result.isOk());
    assertEquals(errorMessage, result.message());
  }

  private Method getTestMethod(PostMethodRequest.MethodSourceEnum source) {
    UUID methodId = UUID.randomUUID();
    Optional<GithubMethodDetails> githubMethodDetails = Optional.empty();
    if (source == PostMethodRequest.MethodSourceEnum.GITHUB) {
      githubMethodDetails =
          Optional.of(new GithubMethodDetails("repo", "org", "path", false, methodId));
    }
    return new Method(
        UUID.randomUUID(),
        "test method",
        "method description ",
        DateUtils.currentTimeInUTC(),
        null,
        source.toString(),
        UUID.randomUUID(),
        githubMethodDetails);
  }

  private MethodVersion getTestMethodVersion(Method method) {
    return new MethodVersion(
        UUID.randomUUID(),
        method,
        "1.0",
        "method version description",
        OffsetDateTime.now(),
        null,
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl",
        method.getOriginalWorkspaceId(),
        "develop",
        Optional.empty());
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
