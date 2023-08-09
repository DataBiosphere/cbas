package bio.terra.cbas.dependencies.sam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;

class TestSamService {
  @SpyBean private SamService samService;
  @MockBean private BearerToken bearerToken;

  private final String tokenWithCorrectAccess = "foo-token";
  private final String validTokenWithNoAccess = "foo-no-access-token";
  private final String validTokenWithReadAccess = "foo-read-access-token";
  private final String validTokenWithWriteAccess = "foo-write-access-token";
  private final String validTokenWithComputeAccess = "foo-compute-access-token";
  private final String expiredTokenValue = "expired-token";
  private final String tokenCausingInterrupt = "interrupting-cow-moo";
  private final String workspaceId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);

  @BeforeEach
  void init() throws ApiException {
    UsersApi usersApi = mock(UsersApi.class);
    ResourcesApi resourcesApi = mock(ResourcesApi.class);
    SamClient samClient = mock(SamClient.class);
    ApiClient apiClient = mock(ApiClient.class);
    samService = spy(new SamService(samClient, bearerToken));

    // setup Sam client methods
    when(samClient.getApiClient(any())).thenReturn(apiClient);
    when(samClient.checkAuthAccessWithSam()).thenReturn(true);
    when(samClient.getWorkspaceId()).thenReturn(workspaceId);

    doReturn(usersApi).when(samService).getUsersApi();
    when(usersApi.getUserStatusInfo())
        .thenAnswer(
            (Answer<UserStatusInfo>)
                invocation -> {
                  if (bearerToken != null
                      && bearerToken.getToken().equals(tokenWithCorrectAccess)) {
                    return mockUser;
                  } else if (bearerToken != null
                      && bearerToken.getToken().equals(tokenCausingInterrupt)) {
                    throw new InterruptedException();
                  } else {
                    // expired or no token
                    throw new ApiException(401, "Unauthorized :(");
                  }
                });

    doReturn(resourcesApi).when(samService).getResourcesApi();

    // mock response for hasReadPermission()
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(samService.READ_ACTION)))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  if (bearerToken != null) {
                    String token = bearerToken.getToken();
                    if (token.equals(validTokenWithReadAccess)
                        || token.equals(validTokenWithWriteAccess)
                        || token.equals(validTokenWithComputeAccess)) return true;
                    if (token.equals(validTokenWithNoAccess)) return false;
                    if (token.equals(tokenCausingInterrupt)) throw new InterruptedException();

                    throw new ApiException(
                        401, "Unauthorized exception thrown for testing purposes");
                  } else {
                    throw new BeanCreationException(
                        "BearerToken bean throws error when no token is available.",
                        new UnauthorizedException("Authorization header missing"));
                  }
                });

    // mock response for hasWritePermission()
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(samService.WRITE_ACTION)))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  if (bearerToken != null) {
                    String token = bearerToken.getToken();
                    if (token.equals(validTokenWithWriteAccess)
                        || token.equals(validTokenWithComputeAccess)) return true;
                    if (token.equals(validTokenWithReadAccess)
                        || token.equals(validTokenWithNoAccess)) return false;
                    if (token.equals(tokenCausingInterrupt)) throw new InterruptedException();

                    throw new ApiException(
                        401, "Unauthorized exception thrown for testing purposes");
                  } else {
                    throw new BeanCreationException(
                        "BearerToken bean throws error when no token is available.",
                        new UnauthorizedException("Authorization header missing"));
                  }
                });

    // mock response for hasComputePermission()
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(samService.COMPUTE_ACTION)))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  if (bearerToken != null) {
                    String token = bearerToken.getToken();
                    if (token.equals(validTokenWithComputeAccess)) return true;
                    if (token.equals(validTokenWithReadAccess)
                        || token.equals(validTokenWithWriteAccess)
                        || token.equals(validTokenWithNoAccess)) return false;
                    if (token.equals(tokenCausingInterrupt)) throw new InterruptedException();

                    throw new ApiException(
                        401, "Unauthorized exception thrown for testing purposes");
                  } else {
                    throw new BeanCreationException(
                        "BearerToken bean throws error when no token is available.",
                        new UnauthorizedException("Authorization header missing"));
                  }
                });
  }

  void setTokenValue(String token) {
    bearerToken = new BearerToken(token);
  }

  @Test
  void testGetSamUserValidToken() {
    setTokenValue(tokenWithCorrectAccess);
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

  // tests for checking read, write and compute access for a token with only read access

  @Test
  void testHasReadPermissionForTokenWithReadAccess() {
    setTokenValue(validTokenWithReadAccess);
    assertTrue(samService.hasReadPermission());
  }

  @Test
  void testHasWritePermissionForTokenWithReadAccess() {
    setTokenValue(validTokenWithReadAccess);
    assertFalse(samService.hasWritePermission());
  }

  @Test
  void testHasComputePermissionForTokenWithReadAccess() {
    setTokenValue(validTokenWithReadAccess);
    assertFalse(samService.hasComputePermission());
  }

  // tests for checking read, write and compute access for a token with write access

  @Test
  void testHasReadPermissionForTokenWithWriteAccess() {
    setTokenValue(validTokenWithWriteAccess);
    assertTrue(samService.hasReadPermission());
  }

  @Test
  void testHasWritePermissionForTokenWithWriteAccess() {
    setTokenValue(validTokenWithWriteAccess);
    assertTrue(samService.hasWritePermission());
  }

  @Test
  void testHasComputePermissionForTokenWithWriteAccess() {
    setTokenValue(validTokenWithWriteAccess);
    assertFalse(samService.hasComputePermission());
  }

  // tests for checking read, write and compute access for a token with compute access

  @Test
  void testHasReadPermissionForTokenWithComputeAccess() {
    setTokenValue(validTokenWithComputeAccess);
    assertTrue(samService.hasReadPermission());
  }

  @Test
  void testHasWritePermissionForTokenWithComputeAccess() {
    setTokenValue(validTokenWithComputeAccess);
    assertTrue(samService.hasWritePermission());
  }

  @Test
  void testHasComputePermissionForTokenWithComputeAccess() {
    setTokenValue(validTokenWithComputeAccess);
    assertTrue(samService.hasComputePermission());
  }

  // tests for checking read, write and compute access for a valid token with no access to current
  // workspace

  @Test
  void testHasReadPermissionForTokenWithNoAccess() {
    setTokenValue(validTokenWithNoAccess);
    assertFalse(samService.hasReadPermission());
  }

  @Test
  void testHasWritePermissionForTokenWithNoAccess() {
    setTokenValue(validTokenWithNoAccess);
    assertFalse(samService.hasWritePermission());
  }

  @Test
  void testHasComputePermissionForTokenWithNoAccess() {
    setTokenValue(validTokenWithNoAccess);
    assertFalse(samService.hasComputePermission());
  }

  @Test
  void testHasPermissionForRequestWithNoToken() {
    // no token set
    BeanCreationException exception =
        assertThrows(BeanCreationException.class, () -> samService.hasReadPermission());

    assertTrue(
        exception
            .getMessage()
            .contains("BearerToken bean throws error when no token is available."));
    assertTrue(exception.getRootCause() instanceof UnauthorizedException);
    assertEquals(exception.getRootCause().getMessage(), "Authorization header missing");
  }

  @Test
  void testHasPermissionForInterruptingToken() {
    setTokenValue(tokenCausingInterrupt);
    SamInterruptedException exception =
        assertThrows(SamInterruptedException.class, () -> samService.hasComputePermission());
    assertEquals(
        "Request interrupted while checking compute permissions on workspace from Sam",
        exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
  }
}
