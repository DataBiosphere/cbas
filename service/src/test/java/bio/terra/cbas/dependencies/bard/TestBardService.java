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
import bio.terra.cbas.config.BardServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import bio.terra.common.iam.BearerToken;
import java.util.Map;
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
  void testBardLogEventDisabled() {
    when(bardServerConfiguration.enabled()).thenReturn(false);
    bardServiceImpl.logEvent("testEvent", Map.of(), userToken);
    verifyNoInteractions(defaultAuthApi);
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
}
