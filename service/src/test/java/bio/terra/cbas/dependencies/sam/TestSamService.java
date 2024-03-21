package bio.terra.cbas.dependencies.sam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dao.util.ContainerizedDaoTest;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

class TestSamService {
  private SamClient samClient;
  @SpyBean private SamService samService;
  @MockBean private BearerToken bearerToken;

  private final String tokenWithCorrectAccess = "foo-token";
  private final String validTokenWithNoAccess = "foo-no-access-token";
  private final String validTokenWithReadAccess = "foo-read-access-token";
  private final String validTokenWithWriteAccess = "foo-write-access-token";
  private final String validTokenWithComputeAccess = "foo-compute-access-token";
  private final String validTokenCausingAccessInterrupt = "moo-access-token";
  private final String expiredTokenValue = "expired-token";
  private final String tokenCausingUserInterrupt = "interrupting-cow-moo";
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
    samClient = mock(SamClient.class);
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
                  if (bearerToken == null || bearerToken.getToken() == null) {
                    throw new BeanCreationException(
                        "BearerToken bean throws error when no token is available.",
                        new UnauthorizedException("Authorization header missing"));
                  }
                  switch (bearerToken.getToken()) {
                    case tokenWithCorrectAccess:
                    case validTokenWithNoAccess:
                    case validTokenWithReadAccess:
                    case validTokenWithWriteAccess:
                    case validTokenWithComputeAccess:
                    case validTokenCausingAccessInterrupt:
                      return mockUser;
                    case tokenCausingUserInterrupt:
                      throw new InterruptedException();
                    default:
                      // expired or invalid
                      throw new ApiException(401, "Unauthorized :(");
                  }
                });

    doReturn(resourcesApi).when(samService).getResourcesApi();

    // mock response for hasReadPermission()
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.READ_ACTION)))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  if (bearerToken != null) {
                    String token = bearerToken.getToken();
                    if (token.equals(validTokenWithReadAccess)
                        || token.equals(validTokenWithWriteAccess)
                        || token.equals(validTokenWithComputeAccess)) return true;
                    if (token.equals(validTokenWithNoAccess)) return false;
                    if (token.equals(validTokenCausingAccessInterrupt))
                      throw new InterruptedException();

                    throw new ApiException(
                        401, "Unauthorized exception thrown for testing purposes");
                  } else {
                    throw new BeanCreationException(
                        "BearerToken bean throws error when no token is available.",
                        new UnauthorizedException("Authorization header missing"));
                  }
                });

    // mock response for hasWritePermission()
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.WRITE_ACTION)))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  if (bearerToken != null) {
                    String token = bearerToken.getToken();
                    if (token.equals(validTokenWithWriteAccess)
                        || token.equals(validTokenWithComputeAccess)) return true;
                    if (token.equals(validTokenWithReadAccess)
                        || token.equals(validTokenWithNoAccess)) return false;
                    if (token.equals(validTokenCausingAccessInterrupt))
                      throw new InterruptedException();

                    throw new ApiException(
                        401, "Unauthorized exception thrown for testing purposes");
                  } else {
                    throw new BeanCreationException(
                        "BearerToken bean throws error when no token is available.",
                        new UnauthorizedException("Authorization header missing"));
                  }
                });

    // mock response for hasComputePermission()
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.COMPUTE_ACTION)))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  if (bearerToken != null) {
                    String token = bearerToken.getToken();
                    if (token.equals(validTokenWithComputeAccess)) return true;
                    if (token.equals(validTokenWithReadAccess)
                        || token.equals(validTokenWithWriteAccess)
                        || token.equals(validTokenWithNoAccess)) return false;
                    if (token.equals(validTokenCausingAccessInterrupt))
                      throw new InterruptedException();

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
    BeanCreationException exception =
        assertThrows(BeanCreationException.class, () -> samService.getSamUser());

    assertTrue(
        exception
            .getMessage()
            .contains("BearerToken bean throws error when no token is available."));
    assertTrue(exception.getRootCause() instanceof UnauthorizedException);
    assertEquals("Authorization header missing", exception.getRootCause().getMessage());
  }

  @Test
  void testGetSamUserExpiredToken() {
    setTokenValue(expiredTokenValue);
    SamUnauthorizedException e =
        assertThrows(SamUnauthorizedException.class, () -> samService.getSamUser());
    assertThat(
        e.getMessage(),
        containsString("Error getting user status info from Sam: Message: Unauthorized :("));
    assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
  }

  @Test
  void testGetSamUserInterruptingToken() {
    setTokenValue(tokenCausingUserInterrupt);
    SamInterruptedException e =
        assertThrows(SamInterruptedException.class, () -> samService.getSamUser());
    assertEquals("Request interrupted while getting user status info from Sam", e.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
  }

  @Test
  void testGetSamUserAuthDisabled() {
    setTokenValue(tokenWithCorrectAccess);
    when(samClient.checkAuthAccessWithSam()).thenReturn(false);
    UserStatusInfo user = samService.getSamUser();
    assertEquals(new UserStatusInfo(), user);
    assertNull(user.getUserSubjectId());
    assertNull(user.getUserEmail());
    assertNull(user.getEnabled());
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
    assertEquals("Authorization header missing", exception.getRootCause().getMessage());
  }

  @Test
  void testHasPermissionForInterruptingToken() {
    setTokenValue(validTokenCausingAccessInterrupt);
    SamInterruptedException exception =
        assertThrows(SamInterruptedException.class, () -> samService.hasComputePermission());
    assertEquals(
        "Request interrupted while checking compute permissions on workspace from Sam",
        exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
  }

  // Tests for including user IDs in logs once permissions have been checked

  @Nested
  @SpringBootTest(
      properties = {
        "spring.profiles.active=human-readable-logging",
        "spring.main.allow-bean-definition-overriding=true"
      })
  @ExtendWith(OutputCaptureExtension.class)
  class TestSamReadableLogs extends ContainerizedDaoTest {

    @Test
    void testUserIdInLogsWithNoAccess(CapturedOutput output) {
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      Logger logger = (Logger) LoggerFactory.getLogger(SamService.class);
      appender.setContext(logger.getLoggerContext());
      logger.setLevel(Level.DEBUG);
      logger.addAppender(appender);
      setTokenValue(validTokenWithNoAccess);
      samService.hasReadPermission();
      assertEquals(mockUser.getUserSubjectId(), MDC.get("user"));
      assertTrue(output.getOut().contains("user=" + mockUser.getUserSubjectId()));
    }

    @Test
    void testUserIdInLogsWithAllAccess(CapturedOutput output) {
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      Logger logger = (Logger) LoggerFactory.getLogger(SamService.class);
      appender.setContext(logger.getLoggerContext());
      logger.setLevel(Level.DEBUG);
      logger.addAppender(appender);
      setTokenValue(validTokenWithComputeAccess);
      samService.hasReadPermission();
      assertEquals(mockUser.getUserSubjectId(), MDC.get("user"));
      assertTrue(output.getOut().contains("user=" + mockUser.getUserSubjectId()));
    }
  }

  @Nested
  @SpringBootTest(
      properties = {"spring.profiles.active=", "spring.main.allow-bean-definition-overriding=true"})
  @ExtendWith(OutputCaptureExtension.class)
  class TestSamPlainLogs extends ContainerizedDaoTest {
    
    @Test
    void testUserIdInPlainLogsWithNoAccess(CapturedOutput output) {
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      Logger logger = (Logger) LoggerFactory.getLogger(SamService.class);
      appender.setContext(logger.getLoggerContext());
      logger.setLevel(Level.DEBUG);
      logger.addAppender(appender);
      setTokenValue(validTokenWithNoAccess);
      samService.hasReadPermission();
      assertEquals(mockUser.getUserSubjectId(), MDC.get("user"));
      assertTrue(output.getOut().contains("\"user\":\"" + mockUser.getUserSubjectId() + "\""));
    }

    @Test
    void testUserIdInPlainLogsWithAllAccess(CapturedOutput output) {
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      Logger logger = (Logger) LoggerFactory.getLogger(SamService.class);
      appender.setContext(logger.getLoggerContext());
      logger.setLevel(Level.DEBUG);
      logger.addAppender(appender);
      setTokenValue(validTokenWithComputeAccess);
      samService.hasReadPermission();
      assertEquals(mockUser.getUserSubjectId(), MDC.get("user"));
      assertTrue(output.getOut().contains("\"user\":\"" + mockUser.getUserSubjectId() + "\""));
    }
  }
}
