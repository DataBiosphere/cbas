package bio.terra.cbas.dependencies.sam;

import static bio.terra.cbas.dependencies.sam.BearerTokenFilter.ATTRIBUTE_NAME_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import bio.terra.common.sam.exception.SamUnauthorizedException;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TestSamService {
  private SamService samService;

  private final String tokenValue = "foo-token";
  private final String expiredTokenValue = "expired-token";
  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);

  @BeforeEach
  void init() throws ApiException {
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
                  if (samService.getUserToken().isPresent()
                      && samService.getUserToken().get().equals(tokenValue)) {
                    return mockUser;
                  } else {
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
    Optional<String> userToken = samService.getUserToken();
    assertTrue(userToken.isPresent());
    assertEquals(tokenValue, userToken.get());
  }

  @Test
  void testGetUserTokenNoToken() {
    setTokenValue(null);
    Optional<String> userToken = samService.getUserToken();
    assertTrue(userToken.isEmpty());
  }

  @Test
  void testGetUserTokenExpiredToken() {
    setTokenValue(expiredTokenValue);
    Optional<String> userToken = samService.getUserToken();
    assertTrue(userToken.isPresent());
    assertEquals(expiredTokenValue, userToken.get());
  }

  @Test
  void testGetSamUser() {
    setTokenValue(tokenValue);
    Optional<UserStatusInfo> user = samService.getSamUser();
    assertTrue(user.isPresent());
    assertEquals(mockUser, user.get());
  }

  @Test
  void testGetSamUserNoToken() {
    setTokenValue(null);
    Optional<UserStatusInfo> user = samService.getSamUser();
    assertTrue(user.isEmpty());
  }

  @Test
  void testGetSamUserExpiredToken() {
    setTokenValue(expiredTokenValue);
    assertThrows(SamUnauthorizedException.class, () -> samService.getSamUser());
  }
}
