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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestBardService {

  @Test
  void testBardLogRunSetEvent() {
    BardClient bardClient = mock(BardClient.class);
    BardService bardService = new BardService(bardClient);
    ApiClient apiClient = mock(ApiClient.class);
    DefaultApi defaultApi = mock(DefaultApi.class);
    BearerToken userToken = new BearerToken("foo");

    when(bardClient.bardAuthClient(any())).thenReturn(apiClient);
    when(bardClient.defaultApi(apiClient)).thenReturn(defaultApi);

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
    verify(defaultApi).eventsEventLog("workflow-submission", "cbas", eventLogRequest);
  }
}
