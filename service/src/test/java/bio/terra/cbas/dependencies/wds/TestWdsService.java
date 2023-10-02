package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.RetryConfig;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.common.iam.BearerToken;
import java.util.UUID;
import javax.ws.rs.ProcessingException;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
class TestWdsService {
  final String baseUri = "http://baseurl.com";
  final String instanceId = UUID.randomUUID().toString();
  final String apiV = "v1";
  final WdsServerConfiguration wdsServerConfiguration =
      new WdsServerConfiguration(baseUri, instanceId, apiV, false);

  final RetryConfig retryConfig = new RetryConfig();
  RetryTemplate template = retryConfig.listenerResetRetryTemplate();

  final BearerToken bearerToken = new BearerToken("");

  @BeforeEach
  void init() {
    FixedBackOffPolicy smallerBackoff = new FixedBackOffPolicy();
    smallerBackoff.setBackOffPeriod(5L); // 5 ms
    template.setBackOffPolicy(smallerBackoff);
  }

  @Test
  void processingExceptionRetriesEventuallySucceed() throws Exception {

    RecordResponse expectedResponse = new RecordResponse().id("foo1").type("FOO");

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);
    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);
    when(recordsApi.getRecord(instanceId, apiV, "FOO", "foo1"))
        .thenThrow(new ProcessingException("Processing exception"))
        .thenReturn(expectedResponse);

    WdsService wdsService =
        new WdsService(wdsClient, wdsServerConfiguration, template, bearerToken);

    assertEquals(expectedResponse, wdsService.getRecord("FOO", "foo1"));
  }

  @Test
  void processingExceptionRetriesEventuallyFail() throws Exception {

    RecordResponse expectedResponse = new RecordResponse().id("foo1").type("FOO");

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);
    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);
    when(recordsApi.getRecord(instanceId, apiV, "FOO", "foo1"))
        .thenThrow(new ProcessingException("Processing exception"))
        .thenThrow(new ProcessingException("Processing exception"))
        .thenThrow(new ProcessingException("Processing exception"));

    WdsService wdsService =
        new WdsService(wdsClient, wdsServerConfiguration, template, bearerToken);

    assertThrows(ProcessingException.class, () -> wdsService.getRecord("FOO", "foo1"));
  }

  @Test
  void otherExceptionsDoNotRetry() throws Exception {

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);
    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);
    when(recordsApi.getRecord(instanceId, apiV, "FOO", "foo1"))
        .thenThrow(new RuntimeException("Other exception"));

    WdsService wdsService =
        new WdsService(wdsClient, wdsServerConfiguration, template, bearerToken);

    assertThrows(RuntimeException.class, () -> wdsService.getRecord("FOO", "foo1"));
  }
}
