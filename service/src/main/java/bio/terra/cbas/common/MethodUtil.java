package bio.terra.cbas.common;

import bio.terra.cbas.common.exceptions.MethodProcessingException.UnknownMethodSourceException;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum;
import bio.terra.cbas.util.methods.GithubUrlDetailsManager.GithubUrlComponents;
import bio.terra.dockstore.model.ToolDescriptor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.EnumUtils;

public final class MethodUtil {
  private static final String GITHUB_URL_HOST = "github.com";
  private static final String RAW_GITHUB_URL_HOST = "raw.githubusercontent.com";
  public static final List<String> SUPPORTED_URL_HOSTS =
      List.of(GITHUB_URL_HOST, RAW_GITHUB_URL_HOST);

  private MethodUtil() {}

  public static String convertToRawUrl(
      String originalUrl,
      MethodSourceEnum methodSource,
      String methodVersion,
      DockstoreService dockstoreService)
      throws URISyntaxException, MalformedURLException, bio.terra.dockstore.client.ApiException {
    return switch (methodSource) {
      case GITHUB -> {
        URL url = new URI(originalUrl).toURL();
        if (url.getHost().equals(RAW_GITHUB_URL_HOST)) {
          yield originalUrl;
        } else {
          yield originalUrl.replace(GITHUB_URL_HOST, RAW_GITHUB_URL_HOST).replace("/blob/", "/");
        }
      }
      case DOCKSTORE -> {
        ToolDescriptor toolDescriptor =
            dockstoreService.descriptorGetV1(originalUrl, methodVersion);
        yield toolDescriptor.getUrl();
      }
    };
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

  public static Boolean verifyIfPrivate(String methodUrl) {
    return false;
  }

  public static GithubUrlComponents extractGithubDetailsFromUrl(String url)
      throws URISyntaxException {
    GithubUrlComponents githubUrlComponents = new GithubUrlComponents();

    URI uri = new URI(url);
    String[] parts = uri.getPath().split("/");
    String[] gitHubPathParts = Arrays.stream(parts).skip(4).toArray(String[]::new);

    githubUrlComponents.setOrganization(parts[1]);
    githubUrlComponents.setRepository(parts[2]);
    githubUrlComponents.setBranchOrTag(parts[3]);
    githubUrlComponents.setPath(String.join("/", gitHubPathParts));

    return githubUrlComponents;
  }
}
