package bio.terra.cbas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestBeanConfig {
  private final BeanConfig beanConfig = new BeanConfig();
  private MockHttpServletRequest request;

  @BeforeEach
  void init() {
    request = new MockHttpServletRequest();
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
}
