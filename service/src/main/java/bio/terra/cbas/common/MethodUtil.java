package bio.terra.cbas.common;

import static bio.terra.cbas.dependencies.github.GitHubService.GITHUB_URL_HOST;
import static bio.terra.cbas.dependencies.github.GitHubService.RAW_GITHUB_URL_HOST;

import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.common.exceptions.MethodProcessingException.UnknownMethodSourceException;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum;
import bio.terra.cbas.util.methods.GithubUrlComponents;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import org.apache.commons.lang3.EnumUtils;

public final class MethodUtil {

  private MethodUtil() {}

  public static String asRawMethodUrlGithub(String originalUrl)
      throws URISyntaxException, MalformedURLException {

    URI uri = new URI(originalUrl);
    URL url;
    if (uri.getScheme() == null) {
      url = new URI("https://" + originalUrl).toURL();
    } else {
      url = uri.toURL();
    }

    if (url.getHost().equals(RAW_GITHUB_URL_HOST)) {
      return originalUrl;
    } else {
      return originalUrl.replace(GITHUB_URL_HOST, RAW_GITHUB_URL_HOST).replace("/blob/", "/");
    }
  }

  public static MethodSourceEnum convertToMethodSourceEnum(String methodSource)
      throws UnknownMethodSourceException {
    // we ignore the case to make it backwards compatible for 3 staged Covid workflows that have
    // 'Github' (instead of 'GitHub') as source. See
    // https://broadworkbench.atlassian.net/browse/WM-2040
    MethodSourceEnum methodSourceEnum =
        EnumUtils.getEnumIgnoreCase(PostMethodRequest.MethodSourceEnum.class, methodSource);
    if (methodSourceEnum != null) {
      return methodSourceEnum;
    }

    throw new UnknownMethodSourceException(methodSource);
  }

  public static GithubUrlComponents extractGithubUrlComponents(String url)
      throws URISyntaxException, MalformedURLException, MethodProcessingException {

    URI uri = new URI(asRawMethodUrlGithub(url));
    String[] parts = uri.getPath().split("/");
    if (parts.length < 4) {
      throw new MethodProcessingException(
          "Github method URL %s is invalid; could not extract repo, org, branch, and/or WDL path."
              .formatted(url));
    }
    String[] gitHubPathParts = Arrays.stream(parts).skip(4).toArray(String[]::new);
    return new GithubUrlComponents(String.join("/", gitHubPathParts), parts[2], parts[1], parts[3]);
  }
}
