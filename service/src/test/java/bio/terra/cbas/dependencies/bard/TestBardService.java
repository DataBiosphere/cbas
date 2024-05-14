package bio.terra.cbas.dependencies.bard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.bard.api.DefaultApi;
import bio.terra.bard.client.ApiClient;
import bio.terra.bard.model.EventsEventLogRequest;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.WdsRecordSet;
import bio.terra.common.iam.BearerToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class TestBardService {
  private BardService bardService;
  private DefaultApi defaultApi;
  private BearerToken userToken;
  private final String appId = "cbas";

  @BeforeEach
  void setup() {
    BardClient bardClient = mock(BardClient.class);
    bardService = new BardService(bardClient);
    ApiClient apiClient = mock(ApiClient.class);
    defaultApi = mock(DefaultApi.class);
    userToken = new BearerToken("foo");
    when(bardClient.bardAuthClient(any())).thenReturn(apiClient);
    when(bardClient.defaultApi(apiClient)).thenReturn(defaultApi);
  }

  @Test
  void testBardLogRunSetEvent() {
    RunSetRequest request =
        new RunSetRequest()
            .runSetName("testRun")
            .methodVersionId(UUID.randomUUID())
            .wdsRecords(new WdsRecordSet().recordIds(List.of("1", "2", "3")));
    bardService.logRunSetEvent(request, userToken);
    HashMap<String, String> properties = new HashMap<>();
    properties.put("runSetName", request.getRunSetName());
    properties.put("methodVersionId", request.getMethodVersionId().toString());
    properties.put("wdsRecords", String.valueOf(request.getWdsRecords().getRecordIds().size()));
    EventsEventLogRequest eventLogRequest = new EventsEventLogRequest().properties(properties);
    verify(defaultApi).eventsEventLog("workflow-submission", appId, eventLogRequest);
  }

  @Test
  void testBardLogEventSuccess() {
    EventsEventLogRequest eventLogRequest = new EventsEventLogRequest().properties(Map.of());
    bardService.logEvent("testEvent", Map.of(), userToken);
    verify(defaultApi).eventsEventLog("testEvent", appId, eventLogRequest);
  }

  @Test
  void testBardLogEventErrorDoesNotThrow() {
    when(defaultApi.eventsEventLog(any(), any(), any()))
        .thenThrow(new RestClientException("API error"));
    EventsEventLogRequest eventLogRequest = new EventsEventLogRequest().properties(Map.of());
    bardService.logEvent("testEvent", Map.of(), userToken);
    verify(defaultApi).eventsEventLog("testEvent", appId, eventLogRequest);
  }
}
