package bio.terra.cbas.controllers;

import static bio.terra.cbas.util.BearerTokenFilter.ATTRIBUTE_NAME_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TestControllerUtils {
  private ControllerUtils utils;

  private final String tokenValue = "foo-token";
  private final String expiredTokenValue = "expired-token";
  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);

  @BeforeEach
  void init() {
    SamService samService = mock(SamService.class);
    when(samService.getUserStatusInfo(tokenValue)).thenReturn(mockUser);
    when(samService.getUserStatusInfo(expiredTokenValue))
        .thenThrow(new SamUnauthorizedException("Unauthorized :("));
    utils = new ControllerUtils(samService);
  }

  void setTokenValue(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(ATTRIBUTE_NAME_TOKEN, token);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @Test
  void testGetUserToken() {
    setTokenValue(tokenValue);
    Optional<String> userToken = utils.getUserToken();
    assertTrue(userToken.isPresent());
    assertEquals(tokenValue, userToken.get());
  }

  @Test
  void testGetUserTokenNoToken() {
    setTokenValue(null);
    Optional<String> userToken = utils.getUserToken();
    assertTrue(userToken.isEmpty());
  }

  @Test
  void testGetUserTokenExpiredToken() {
    setTokenValue(expiredTokenValue);
    Optional<String> userToken = utils.getUserToken();
    assertTrue(userToken.isPresent());
    assertEquals(expiredTokenValue, userToken.get());
  }

  @Test
  void testGetSamUser() {
    setTokenValue(tokenValue);
    Optional<UserStatusInfo> user = utils.getSamUser();
    assertTrue(user.isPresent());
    assertEquals(mockUser, user.get());
  }

  @Test
  void testGetSamUserNoToken() {
    setTokenValue(null);
    Optional<UserStatusInfo> user = utils.getSamUser();
    assertTrue(user.isEmpty());
  }

  @Test
  void testGetSamUserExpiredToken() {
    setTokenValue(expiredTokenValue);
    assertThrows(SamUnauthorizedException.class, () -> utils.getSamUser());
  }
}
