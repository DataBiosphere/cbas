package bio.terra.cbas.util;

import static bio.terra.cbas.dependencies.common.BearerTokenFilter.ATTRIBUTE_NAME_TOKEN;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class TokenUtil {

  /**
   * Look in RequestContextHolder for a non-null String ATTRIBUTE_NAME_TOKEN
   *
   * @return the token if found; BearerToken.empty() otherwise
   */
  public static String tokenFromRequestContext() {
    // do any request attributes exist?
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null) {
      // TODO: maybe instead of returning String convert into a typed-class object
      return requestAttributes.getAttribute(ATTRIBUTE_NAME_TOKEN, SCOPE_REQUEST).toString();
    }
    return null;
  }
}
