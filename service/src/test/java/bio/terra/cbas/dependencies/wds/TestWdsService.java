package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.RetryConfig;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.wds.WdsService.WdsRecordResponseDetails;
import bio.terra.common.iam.BearerToken;
import jakarta.ws.rs.ProcessingException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.databiosphere.workspacedata.api.RecordsApi;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordAttributes;
import org.databiosphere.workspacedata.model.RecordQueryResponse;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
class TestWdsService {
  final String baseUri = "http://baseurl.com";
  final String instanceId = UUID.randomUUID().toString();
  final String apiV = "v1";
  final WdsServerConfiguration wdsServerConfiguration =
      new WdsServerConfiguration(baseUri, instanceId, apiV, 1000, false);

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

    WdsService wdsService = new WdsService(wdsClient, wdsServerConfiguration, template);

    assertEquals(expectedResponse, wdsService.getRecord("FOO", "foo1", bearerToken));
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

    WdsService wdsService = new WdsService(wdsClient, wdsServerConfiguration, template);

    assertThrows(ProcessingException.class, () -> wdsService.getRecord("FOO", "foo1", bearerToken));
  }

  @Test
  void otherExceptionsDoNotRetry() throws Exception {

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);
    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);
    when(recordsApi.getRecord(instanceId, apiV, "FOO", "foo1"))
        .thenThrow(new RuntimeException("Other exception"));

    WdsService wdsService = new WdsService(wdsClient, wdsServerConfiguration, template);

    assertThrows(RuntimeException.class, () -> wdsService.getRecord("FOO", "foo1", bearerToken));
  }

  record BatchTestCase(
      List<String> requestedIds,
      Integer wdsBatchSize,
      List<List<String>> anticipatedRequestBatches) {}

  static List<String> foosList(int startIndex, int count) {
    return Stream.iterate(startIndex, i -> i + 1)
        .limit(count)
        .map(i -> "foo" + i)
        .toList();
  }

  static Stream<BatchTestCase> batchTestCases() {
    return Stream.of(
        new BatchTestCase(List.of("foo1"), 1000, List.of(List.of("foo1"))),
        new BatchTestCase(foosList(0, 2), 1000, List.of(foosList(0, 2))),
        new BatchTestCase(foosList(0, 1000), 1000, List.of(foosList(0, 1000))),
        new BatchTestCase(foosList(0, 1001), 1000, List.of(foosList(0, 1000), foosList(1000, 1))),
        new BatchTestCase(
            foosList(0, 10001),
            1000,
            List.of(
                foosList(0, 1000),
                foosList(1000, 1000),
                foosList(2000, 1000),
                foosList(3000, 1000),
                foosList(4000, 1000),
                foosList(5000, 1000),
                foosList(6000, 1000),
                foosList(7000, 1000),
                foosList(8000, 1000),
                foosList(9000, 1000),
                foosList(10000, 1))));
  }

  @ParameterizedTest
  @MethodSource("batchTestCases")
  void sendsRequestsInBatches(BatchTestCase batchTestCase) throws Exception {

    List<String> requestedIds = batchTestCase.requestedIds();
    Map<String, RecordResponse> recordsInWds =
        requestedIds.stream()
            .collect(
                Collectors.toMap(
                    id -> id,
                    id -> {
                      RecordAttributes attributes = new RecordAttributes();
                      attributes.put("id", id);
                      return new RecordResponse().id(id).type("FOO").attributes(attributes);
                    }));
    List<List<RecordResponse>> returnedRecordBatches = new ArrayList<>();
    for (List<String> ids : batchTestCase.anticipatedRequestBatches()) {
      List<RecordResponse> collect =
          ids.stream().map(recordsInWds::get).toList();
      returnedRecordBatches.add(collect);
    }

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);

    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);

    var queryRecordsStub =
        when(recordsApi.queryRecords(any(), eq(instanceId), eq(apiV), eq("FOO")));
    for (List<RecordResponse> returnedRecords : returnedRecordBatches) {
      queryRecordsStub =
          queryRecordsStub.thenReturn(new RecordQueryResponse().records(returnedRecords));
    }

    WdsService wdsService = new WdsService(wdsClient, wdsServerConfiguration, template);

    WdsRecordResponseDetails responseDetails =
        wdsService.getRecords("FOO", requestedIds, bearerToken);

    assertEquals(requestedIds.size(), responseDetails.recordResponseList().size());
    assertEquals(
        Set.copyOf(recordsInWds.values()), Set.copyOf(responseDetails.recordResponseList()));
  }

  @Test
  void catchesMissingRecord() throws Exception {
    List<String> requestedIds = List.of("foo1", "foo2", "foo3");
    List<RecordResponse> returnedRecords =
        List.of(
            new RecordResponse().id("foo1").type("FOO"),
            new RecordResponse().id("foo3").type("FOO"));

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);

    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);

    when(recordsApi.queryRecords(any(), eq(instanceId), eq(apiV), eq("FOO")))
        .thenReturn(new RecordQueryResponse().records(returnedRecords));

    WdsService wdsService = new WdsService(wdsClient, wdsServerConfiguration, template);

    WdsRecordResponseDetails responseDetails =
        wdsService.getRecords("FOO", requestedIds, bearerToken);

    assertEquals(2, responseDetails.recordResponseList().size());
    assertEquals(1, responseDetails.recordIdsWithError().size());
    assertEquals(Set.copyOf(returnedRecords), Set.copyOf(responseDetails.recordResponseList()));
    assertEquals(Map.of("foo2", "Record not found"), responseDetails.recordIdsWithError());
  }

  @Test
  void catchesErrorsThrownDuringBatchFetch() throws Exception {
    List<String> requestedIds = List.of("foo1", "foo2", "foo3");

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);

    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);

    WdsServerConfiguration lowBatchConfig =
        new WdsServerConfiguration(baseUri, instanceId, apiV, 2, false);
    when(recordsApi.queryRecords(any(), eq(instanceId), eq(apiV), eq("FOO")))
        .thenReturn(
            new RecordQueryResponse()
                .records(
                    List.of(
                        new RecordResponse().id("foo1").type("FOO"),
                        new RecordResponse().id("foo2").type("FOO"))))
        .thenThrow(new ApiException(500, "Error fetching records"));

    WdsService wdsService = new WdsService(wdsClient, lowBatchConfig, template);

    WdsRecordResponseDetails responseDetails =
        wdsService.getRecords("FOO", requestedIds, bearerToken);

    assertEquals(2, responseDetails.recordResponseList().size());
    assertEquals(1, responseDetails.recordIdsWithError().size());
    String anticipatedMessage =
        """
                        Message: Error fetching records
                        HTTP response code: 500
                        HTTP response body: null
                        HTTP response headers: null""";
    assertEquals(Map.of("foo3", anticipatedMessage), responseDetails.recordIdsWithError());
  }

  @Test
  void recoversSingleErrorsThrownDuringBatchFetch() throws Exception {
    List<String> requestedIds = List.of("foo1", "foo2", "foo3");

    WdsClient wdsClient = mock(WdsClient.class);
    RecordsApi recordsApi = mock(RecordsApi.class);

    when(wdsClient.recordsApi(any())).thenReturn(recordsApi);

    when(recordsApi.queryRecords(any(), eq(instanceId), eq(apiV), eq("FOO")))
        .thenThrow(new ProcessingException("Listener error"))
        .thenReturn(
            new RecordQueryResponse()
                .records(
                    List.of(
                        new RecordResponse().id("foo1").type("FOO"),
                        new RecordResponse().id("foo2").type("FOO"),
                        new RecordResponse().id("foo3").type("FOO"))));

    WdsService wdsService = new WdsService(wdsClient, wdsServerConfiguration, template);

    WdsRecordResponseDetails responseDetails =
        wdsService.getRecords("FOO", requestedIds, bearerToken);

    assertEquals(3, responseDetails.recordResponseList().size());
    assertEquals(0, responseDetails.recordIdsWithError().size());
  }
}
