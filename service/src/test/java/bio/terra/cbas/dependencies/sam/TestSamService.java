package bio.terra.cbas.dependencies.sam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;

class TestSamService {
  @SpyBean private SamService samService;
  @MockBean private BearerToken bearerToken;
  @MockBean private UsersApi usersApi;

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
    usersApi = mock(UsersApi.class);
    SamClient samClient = mock(SamClient.class);
    samService = spy(new SamService(samClient, bearerToken));
    doReturn(usersApi).when(samService).getUsersApi();
    when(usersApi.getUserStatusInfo())
        .thenAnswer(
            (Answer<UserStatusInfo>)
                invocation -> {
                  if (bearerToken != null && bearerToken.getToken().equals(tokenValue)) {
                    return mockUser;
                  } else if (bearerToken != null
                      && bearerToken.getToken().equals(tokenCausingInterrupt)) {
                    throw new InterruptedException();
                  } else {
                    // expired or no token
                    throw new ApiException(401, "Unauthorized :(");
                  }
                });
  }

  void setTokenValue(String token) {
    bearerToken = new BearerToken(token);
  }

  @Test
  void testGetSamUserValidToken() {
    setTokenValue(tokenValue);
    UserStatusInfo user = samService.getSamUser();
    assertEquals(mockUser, user);
  }

  @Test
  void testGetSamUserNoToken() {
    setTokenValue("");
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
