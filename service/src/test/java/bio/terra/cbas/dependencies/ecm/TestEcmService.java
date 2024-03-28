package bio.terra.cbas.dependencies.ecm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.BearerToken;
import bio.terra.externalcreds.api.OauthApi;
import bio.terra.externalcreds.client.ApiClient;
import bio.terra.externalcreds.model.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestEcmService {

  @Test
  void ecmReturnsExpectedToken() {
    EcmClient ecmClient = mock(EcmClient.class);
    BearerToken bearerToken = new BearerToken("foo");
    String token = "token_here";
    EcmService ecmService = new EcmService(ecmClient, bearerToken);
    ApiClient apiClient = mock(ApiClient.class);
    OauthApi oAuthApi = mock(OauthApi.class);

    when(ecmClient.oAuthApi(any())).thenReturn(oAuthApi);
    when(ecmClient.ecmAuthClient(bearerToken.getToken())).thenReturn(apiClient);
    when(ecmClient.oAuthApi(apiClient).getProviderAccessToken(Provider.GITHUB)).thenReturn(token);

    assertEquals(token, ecmService.getAccessToken());
  }
}
