package bio.terra.cbas.util;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Servlet filter that inspects the incoming request for an LeoToken=... cookie, and saves any token
 * it finds into the current thread-local RequestContextHolder.
 *
 * <p>Note that this filter does not validate or inspect the token; it just extracts it from the
 * request, allowing it to be sent as-is from CBAS to other services such as Sam.
 *
 * <p>Adapted from <a
 * href="https://github.com/DataBiosphere/terra-workspace-data-service/blob/b479672d36545d0b45766b3e0e09564d077a19b8/service/src/main/java/org/databiosphere/workspacedataservice/sam/BearerTokenFilter.java">WDS</a>
 */
@Component
public class BearerTokenFilter implements Filter {
  public static final String ATTRIBUTE_NAME_TOKEN = "bearer-token-attribute";
  private static final String COOKIE_NAME = "LeoToken";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // ServletRequest does not expose cookie methods
    if (request instanceof HttpServletRequest httpRequest) {
      Cookie[] cookies = Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]);
      Optional<Cookie> leoToken =
          Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(COOKIE_NAME)).findFirst();
      if (leoToken.isPresent()) {
        String token = leoToken.get().getValue();

        RequestAttributes currentAttributes = RequestContextHolder.currentRequestAttributes();
        currentAttributes.setAttribute(ATTRIBUTE_NAME_TOKEN, token, SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(currentAttributes);
      }
    }

    chain.doFilter(request, response);
  }
}
