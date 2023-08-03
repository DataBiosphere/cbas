package bio.terra.cbas.dependencies.sam;

import static bio.terra.cbas.dependencies.sam.BearerTokenFilter.ATTRIBUTE_NAME_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TestSamService {
  private SamService samService;

  private final String tokenValue = "foo-token";
  private final String expiredTokenValue = "expired-token";
  private final String tokenCausingInterrupt = "interrupting-cow-moo";
  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);

  @BeforeEach
  void init() throws ApiException {
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
    UsersApi usersApi = mock(UsersApi.class);
    ApiClient apiClient = mock(ApiClient.class);
    SamClient samClient = mock(SamClient.class);
    samService = spy(new SamService(samClient));
    when(samClient.getApiClient(any())).thenReturn(apiClient);
    doReturn(usersApi).when(samService).getUsersApi(any());
    when(usersApi.getUserStatusInfo())
        .thenAnswer(
            (Answer<UserStatusInfo>)
                invocation -> {
                  String token = samService.getUserToken();
                  if (token != null && token.equals(tokenValue)) {
                    return mockUser;
                  } else if (token != null && token.equals(tokenCausingInterrupt)) {
                    throw new InterruptedException();
                  } else {
                    // expired or no token
                    throw new ApiException(401, "Unauthorized :(");
                  }
                });
  }

  void setTokenValue(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(ATTRIBUTE_NAME_TOKEN, token);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @Test
  void testGetUserToken() {
    setTokenValue(tokenValue);
    String userToken = samService.getUserToken();
    assertEquals(tokenValue, userToken);
  }

  @Test
  void testGetSamUserNoToken() {
    SamUnauthorizedException e =
        assertThrows(SamUnauthorizedException.class, () -> samService.getSamUser());
    assertEquals("Error getting user status info from Sam: Unauthorized :(", e.getMessage());
    assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
  }

  @Test
  void testGetSamUserExpiredToken() {
    setTokenValue(expiredTokenValue);
    SamUnauthorizedException e =
        assertThrows(SamUnauthorizedException.class, () -> samService.getSamUser());
    assertEquals("Error getting user status info from Sam: Unauthorized :(", e.getMessage());
    assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
  }

  @Test
  void testGetSamUserInterruptingToken() {
    setTokenValue(tokenCausingInterrupt);
    SamInterruptedException e =
        assertThrows(SamInterruptedException.class, () -> samService.getSamUser());
    assertEquals("Request interrupted while getting user status info from Sam", e.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
  }
}
