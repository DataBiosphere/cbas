package bio.terra.cbas.dependencies.leonardo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import bio.terra.cbas.config.RetryConfig;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.common.iam.BearerToken;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.api.AppsApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class TestLeonardoService {
  final String workspaceId = UUID.randomUUID().toString();
  final WdsServerConfiguration wdsServerConfiguration =
      new WdsServerConfiguration("baseUri", workspaceId, "v1", false);

  final RetryConfig retryConfig = new RetryConfig();

  final BearerToken bearerToken = new BearerToken("");

  final Answer<Object> errorAnswer =
      invocation -> {
        throw new SocketTimeoutException("Timeout");
      };

  @Test
  void processingExceptionRetriesEventuallySucceed() throws Exception {
    List<ListAppResponse> expectedResponse = List.of(new ListAppResponse());

    LeonardoClient leonardoClient = mock(LeonardoClient.class);
    AppsApi appsApi = mock(AppsApi.class);
    when(appsApi.listAppsV2(workspaceId, null, null, null))
        .thenAnswer(errorAnswer)
        .thenReturn(expectedResponse);

    LeonardoService leonardoService =
        spy(
            new LeonardoService(
                leonardoClient,
                wdsServerConfiguration,
                retryConfig.listenerResetRetryTemplate(),
                bearerToken));

    doReturn(appsApi).when(leonardoService).getAppsApi();

    assertEquals(expectedResponse, leonardoService.getApps());
  }

  @Test
  void processingExceptionRetriesEventuallyFail() throws Exception {
    List<ListAppResponse> expectedResponse = List.of(new ListAppResponse());

    LeonardoClient leonardoClient = mock(LeonardoClient.class);
    AppsApi appsApi = mock(AppsApi.class);
    when(appsApi.listAppsV2(workspaceId, null, null, null))
        .thenAnswer(errorAnswer)
        .thenAnswer(errorAnswer)
        .thenAnswer(errorAnswer)
        .thenReturn(expectedResponse);

    LeonardoService leonardoService =
        spy(
            new LeonardoService(
                leonardoClient,
                wdsServerConfiguration,
                retryConfig.listenerResetRetryTemplate(),
                bearerToken));

    doReturn(appsApi).when(leonardoService).getAppsApi();

    assertThrows(SocketTimeoutException.class, leonardoService::getApps);
  }

  @Test
  void apiExceptionsDoNotRetry() throws Exception {
    List<ListAppResponse> expectedResponse = List.of(new ListAppResponse());

    ApiException expectedException = new ApiException(400, "Bad Leonardo");

    LeonardoClient leonardoClient = mock(LeonardoClient.class);
    AppsApi appsApi = mock(AppsApi.class);
    when(appsApi.listAppsV2(workspaceId, null, null, null))
        .thenThrow(expectedException)
        .thenReturn(expectedResponse);

    LeonardoService leonardoService =
        spy(
            new LeonardoService(
                leonardoClient,
                wdsServerConfiguration,
                retryConfig.listenerResetRetryTemplate(),
                bearerToken));

    doReturn(appsApi).when(leonardoService).getAppsApi();

    LeonardoServiceApiException thrown =
        assertThrows(LeonardoServiceApiException.class, leonardoService::getApps);
    assertEquals(expectedException, thrown.getCause());
  }
}
