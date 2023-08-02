package bio.terra.cbas.dependencies.sam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TestBearerTokenFilter {
  private static final BearerTokenFilter filter = new BearerTokenFilter();

  private ServletResponse response;
  private FilterChain chain;

  @BeforeEach
  void initMocks() {
    RequestContextHolder.resetRequestAttributes();
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
  }

  @Test
  void testRequestWithAuthHeader() throws ServletException, IOException {
    String tokenValue = "foo-token";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", tokenValue));
    assertNull(request.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    filter.doFilter(request, response, chain);
    assertEquals(request.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN), tokenValue);
  }

  @Test
  void testRequestWithAuthHeaderNoBearer() throws ServletException, IOException {
    String tokenValue = "foo-token";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, tokenValue);
    assertNull(request.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    filter.doFilter(request, response, chain);
    assertNull(request.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN));
  }

  @Test
  void testRequestWithoutAuthHeader() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    assertNull(request.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    filter.doFilter(request, response, chain);
    assertNull(request.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN));
  }

  @Test
  void testNonHttpRequest() throws ServletException, IOException {
    String tokenValue = "foo-token";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", tokenValue));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    ServletRequest wrappedRequest = new ServletRequestWrapper(request);
    filter.doFilter(wrappedRequest, response, chain);
    assertNull(request.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN));
  }
}
