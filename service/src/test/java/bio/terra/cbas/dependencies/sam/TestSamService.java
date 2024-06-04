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

import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

class TestSamService {
  private final SamClient samClient = mock(SamClient.class);
  private final UsersApi usersApi = mock(UsersApi.class);
  private final ResourcesApi resourcesApi = mock(ResourcesApi.class);
  @SpyBean private final SamService samService = spy(new SamService(samClient));

  private final BearerToken tokenWithCorrectAccess = new BearerToken("foo-token");
  private final BearerToken validTokenWithNoAccess = new BearerToken("foo-no-access-token");
  private final BearerToken validTokenWithReadAccess = new BearerToken("foo-read-access-token");
  private final BearerToken validTokenWithWriteAccess = new BearerToken("foo-write-access-token");
  private final BearerToken validTokenCausingAccessInterrupt = new BearerToken("moo-access-token");
  private final BearerToken expiredTokenValue = new BearerToken("expired-token");
  private final BearerToken tokenCausingUserInterrupt = new BearerToken("interrupting-cow-moo");
  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);

  void initForHasPermission() throws ApiException {
    when(samClient.checkAuthAccessWithSam()).thenReturn(true);
    doReturn(usersApi).when(samService).getUsersApi(any());
    when(usersApi.getUserStatusInfo()).thenReturn(mockUser);
  }

  @Test
  void testGetSamUserValidToken() throws ApiException {
    when(samClient.checkAuthAccessWithSam()).thenReturn(true);
    doReturn(usersApi).when(samService).getUsersApi(tokenWithCorrectAccess);
    when(usersApi.getUserStatusInfo()).thenReturn(mockUser);

    UserStatusInfo user = samService.getSamUser(tokenWithCorrectAccess);
    assertEquals(mockUser, user);
  }

  @Test
  void testGetSamUserExpiredToken() throws ApiException {
    when(samClient.checkAuthAccessWithSam()).thenReturn(true);
    doReturn(usersApi).when(samService).getUsersApi(expiredTokenValue);
    when(usersApi.getUserStatusInfo()).thenThrow(new ApiException(401, "Unauthorized :("));

    SamUnauthorizedException e =
        assertThrows(
            SamUnauthorizedException.class, () -> samService.getSamUser(expiredTokenValue));
    assertThat(
        e.getMessage(),
        containsString("Error getting user status info from Sam: Message: Unauthorized :("));
    assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
  }

  @Test
  void testGetSamUserInterruptingToken() throws ApiException {
    when(samClient.checkAuthAccessWithSam()).thenReturn(true);
    doReturn(usersApi).when(samService).getUsersApi(tokenCausingUserInterrupt);
    when(usersApi.getUserStatusInfo())
        .thenAnswer(
            (Answer<UserStatusInfo>)
                invocation -> {
                  throw new InterruptedException();
                });

    SamInterruptedException e =
        assertThrows(
            SamInterruptedException.class, () -> samService.getSamUser(tokenCausingUserInterrupt));
    assertEquals("Request interrupted while getting user status info from Sam", e.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
  }

  @Test
  void testGetSamUserAuthDisabled() {
    when(samClient.checkAuthAccessWithSam()).thenReturn(false);
    UserStatusInfo user = samService.getSamUser(tokenWithCorrectAccess);
    assertEquals(new UserStatusInfo(), user);
    assertNull(user.getUserSubjectId());
    assertNull(user.getUserEmail());
    assertNull(user.getEnabled());
  }

  // tests for checking read and write access for a token with only read access

  @Test
  void testHasReadPermissionForTokenWithReadAccess() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(validTokenWithReadAccess);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.READ_ACTION)))
        .thenReturn(true);

    assertTrue(samService.hasReadPermission(validTokenWithReadAccess));
  }

  @Test
  void testHasWritePermissionForTokenWithReadAccess() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(validTokenWithReadAccess);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.WRITE_ACTION)))
        .thenReturn(false);

    assertFalse(samService.hasWritePermission(validTokenWithReadAccess));
  }

  // tests for checking read and write access for a token with write access

  @Test
  void testHasReadPermissionForTokenWithWriteAccess() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(validTokenWithWriteAccess);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.READ_ACTION)))
        .thenReturn(true);

    assertTrue(samService.hasReadPermission(validTokenWithWriteAccess));
  }

  @Test
  void testHasWritePermissionForTokenWithWriteAccess() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(validTokenWithWriteAccess);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.WRITE_ACTION)))
        .thenReturn(true);

    assertTrue(samService.hasWritePermission(validTokenWithWriteAccess));
  }

  // tests for checking read and write access for a valid token with no access to current
  // workspace

  @Test
  void testHasReadPermissionForTokenWithNoAccess() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(validTokenWithNoAccess);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.READ_ACTION)))
        .thenReturn(false);

    assertFalse(samService.hasReadPermission(validTokenWithNoAccess));
  }

  @Test
  void testHasWritePermissionForTokenWithNoAccess() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(validTokenWithNoAccess);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.WRITE_ACTION)))
        .thenReturn(false);

    assertFalse(samService.hasWritePermission(validTokenWithNoAccess));
  }

  @Test
  void testHasPermissionForInterruptingToken() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(validTokenCausingAccessInterrupt);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.READ_ACTION)))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  throw new InterruptedException();
                });

    SamInterruptedException exception =
        assertThrows(
            SamInterruptedException.class,
            () -> samService.hasReadPermission(validTokenCausingAccessInterrupt));
    assertEquals(
        "Request interrupted while checking read permissions on workspace from Sam",
        exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
  }

  @Test
  void testHasPermissionForUnauthorizedToken() throws ApiException {
    initForHasPermission();

    doReturn(resourcesApi).when(samService).getResourcesApi(expiredTokenValue);
    when(resourcesApi.resourcePermissionV2(any(), any(), eq(SamService.READ_ACTION)))
        .thenThrow(new ApiException(401, "Unauthorized :("));

    SamUnauthorizedException e =
        assertThrows(
            SamUnauthorizedException.class, () -> samService.hasReadPermission(expiredTokenValue));
    assertThat(
        e.getMessage(),
        containsString(
            "Error checking read permissions on workspace from Sam: Message: Unauthorized :("));
    assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
  }

  // Tests for including user IDs in logs once permissions have been checked

  @Nested
  @SpringBootTest
  @ActiveProfiles({"test", "human-readable-logging"})
  @ExtendWith(OutputCaptureExtension.class)
  class TestSamReadableLogs {

    @Test
    void testUserIdInLogsWithNoAccess(CapturedOutput output) {
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      Logger logger = (Logger) LoggerFactory.getLogger(SamService.class);
      appender.setContext(logger.getLoggerContext());
      logger.setLevel(Level.DEBUG);
      logger.addAppender(appender);
      samService.hasReadPermission(validTokenWithNoAccess);
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
      samService.hasReadPermission(validTokenWithWriteAccess);
      assertEquals(mockUser.getUserSubjectId(), MDC.get("user"));
      assertTrue(output.getOut().contains("user=" + mockUser.getUserSubjectId()));
    }
  }

  @Nested
  @SpringBootTest
  @ActiveProfiles("test")
  @ExtendWith(OutputCaptureExtension.class)
  class TestSamPlainLogs {

    @Test
    void testUserIdInPlainLogsWithNoAccess(CapturedOutput output) {
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      Logger logger = (Logger) LoggerFactory.getLogger(SamService.class);
      appender.setContext(logger.getLoggerContext());
      logger.setLevel(Level.DEBUG);
      logger.addAppender(appender);
      samService.hasReadPermission(validTokenWithNoAccess);
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
      samService.hasReadPermission(validTokenWithWriteAccess);
      assertEquals(mockUser.getUserSubjectId(), MDC.get("user"));
      assertTrue(output.getOut().contains("\"user\":\"" + mockUser.getUserSubjectId() + "\""));
    }
  }
}
