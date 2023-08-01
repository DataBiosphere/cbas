package bio.terra.cbas.dependencies.sam;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * <a href="https://docs.oracle.com/javaee/6/api/javax/servlet/Filter.html">Servlet filter</a> that
 * inspects the incoming request for an Authorization: Bearer ... token, and saves any token it
 * finds into the current thread-local RequestContextHolder.
 *
 * <p>Filters are contructed once (hence the @Component tag), while doFilter is called for each
 * request/response pair. Read the Javadoc for `doFilter` to learn more about its function. By
 * isolating the request to this class, we can access thread-specific RequestContextHolders without
 * worrying about whether a given `request` object is up-to-date. The filter is necessary to inspect
 * certain request properties like headers, while the RequestContextHolders serves more as a
 * dictionary for specific keyed attributes.
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
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // ServletRequest does not expose header methods
    if (request instanceof HttpServletRequest httpRequest) {
      String authString =
          Optional.ofNullable(httpRequest.getHeader(HttpHeaders.AUTHORIZATION)).orElse("");
      if (authString.startsWith(BEARER_PREFIX)) {
        String token = authString.replaceFirst(BEARER_PREFIX, "");

        RequestAttributes currentAttributes = RequestContextHolder.currentRequestAttributes();
        currentAttributes.setAttribute(
            ATTRIBUTE_NAME_TOKEN, token, RequestAttributes.SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(currentAttributes);
      }
    }

    chain.doFilter(request, response);
  }
}
