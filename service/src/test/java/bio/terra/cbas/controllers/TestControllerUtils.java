package bio.terra.cbas.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.util.BearerTokenFilter;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import java.util.Optional;
import javax.servlet.ServletRequest;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestControllerUtils {
  private ServletRequest request;
  private ControllerUtils utils;

  private final String tokenValue = "foo-leo-token";
  private final String expiredTokenValue = "expired-leo-token";
  private final UserStatusInfo mockUser =
      new UserStatusInfo()
          .userEmail("realuser@gmail.com")
          .userSubjectId("user-id-foo")
          .enabled(true);

  @BeforeEach
  void init() throws InterruptedException {
    request = mock(MockHttpServletRequest.class);
    SamService samService = mock(SamService.class);
    when(samService.getUserStatusInfo(eq(tokenValue))).thenReturn(mockUser);
    when(samService.getUserStatusInfo(eq(expiredTokenValue)))
        .thenThrow(new SamUnauthorizedException("Unauthorized :("));
    utils = new ControllerUtils(request, samService);
  }

  void addToken() {
    when(request.getAttribute(eq(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN))).thenReturn(tokenValue);
  }

  void addExpiredToken() {
    when(request.getAttribute(eq(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN)))
        .thenReturn(expiredTokenValue);
  }

  void noToken() {
    when(request.getAttribute(eq(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN))).thenReturn(null);
  }

  @Test
  void testGetUserToken() {
    addToken();
    Optional<String> userToken = utils.getUserToken();
    assertTrue(userToken.isPresent());
    assertEquals(tokenValue, userToken.get());
  }

  @Test
  void testGetUserTokenNoToken() {
    noToken();
    Optional<String> userToken = utils.getUserToken();
    assertTrue(userToken.isEmpty());
  }

  @Test
  void testGetUserTokenExpiredToken() {
    addExpiredToken();
    Optional<String> userToken = utils.getUserToken();
    assertTrue(userToken.isPresent());
    assertEquals(expiredTokenValue, userToken.get());
  }

  @Test
  void testGetSamUser() {
    addToken();
    Optional<UserStatusInfo> user = utils.getSamUser();
    assertTrue(user.isPresent());
    assertEquals(mockUser, user.get());
  }

  @Test
  void testGetSamUserNoToken() {
    noToken();
    Optional<UserStatusInfo> user = utils.getSamUser();
    assertTrue(user.isEmpty());
  }

  @Test
  void testGetSamUserExpiredToken() {
    addExpiredToken();
    assertThrows(SamUnauthorizedException.class, () -> utils.getSamUser());
  }
}
