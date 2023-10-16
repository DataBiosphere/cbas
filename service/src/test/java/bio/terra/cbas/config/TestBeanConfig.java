package bio.terra.cbas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dependencies.sam.SamClient;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.exception.SamForbiddenException;
import bio.terra.common.sam.exception.SamInterruptedException;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.ApiResponse;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class TestBeanConfig {
  private final BeanConfig beanConfig = new BeanConfig();
  private MockHttpServletRequest request;
  @MockBean private ApiClient samApiClient;
  final SamServerConfiguration samServerConfiguration =
      new SamServerConfiguration("baseUri", false, "workspace-id", true);
  @SpyBean private SamClient samClient;

  @BeforeEach
  void init() {
    request = new MockHttpServletRequest();
    samApiClient = mock(ApiClient.class);
    samClient = spy(new SamClient(samServerConfiguration));
    doReturn(samApiClient).when(samClient).getApiClient(any());
  }

  @Test
  void bearerToken() {
    String token = "token";
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    assertEquals(token, beanConfig.bearerToken(request).getToken());
  }

  @Test
  void noBearerToken() {
    assertThrows(UnauthorizedException.class, () -> beanConfig.bearerToken(request));
  }

  @Test
  void testGetSamUserValidToken() throws ApiException {
    doReturn(new ApiResponse<>(400, Map.of(), new UserStatusInfo().userSubjectId("foo-bar")))
        .when(samApiClient)
        .execute(any(), any());
    UserStatusInfo user = beanConfig.userInfo(samClient, new BearerToken("token-baz"));
    assertEquals("foo-bar", user.getUserSubjectId());
    assertNull(user.getEnabled());
    assertNull(user.getUserEmail());
  }

  @Test
  void testGetSamUserAuthDisabled() {
    when(samClient.checkAuthAccessWithSam()).thenReturn(false);
    UserStatusInfo user = beanConfig.userInfo(samClient, null);
    assertNull(user.getUserSubjectId());
    assertNull(user.getEnabled());
    assertNull(user.getUserEmail());
  }

  @Test
  void testGetSamUserUnauthorized() throws ApiException {
    doThrow(new ApiException(403, "Forbidden")).when(samApiClient).execute(any(), any());
    BearerToken token = new BearerToken("token-baz");

    SamForbiddenException e =
        assertThrows(SamForbiddenException.class, () -> beanConfig.userInfo(samClient, token));
    assertEquals("Error getting user status info from Sam: Forbidden", e.getMessage());
    assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
  }

  @Test
  void testGetSamUserInterrupted() throws ApiException {
    doAnswer(
            (invocation) -> {
              throw new InterruptedException();
            })
        .when(samApiClient)
        .execute(any(), any());
    BearerToken token = new BearerToken("token-baz");

    SamInterruptedException e =
        assertThrows(SamInterruptedException.class, () -> beanConfig.userInfo(samClient, token));
    assertEquals("Request interrupted while getting user status info from Sam", e.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
  }
}
