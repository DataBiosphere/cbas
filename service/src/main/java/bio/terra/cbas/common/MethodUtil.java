package bio.terra.cbas.common;

import bio.terra.cbas.common.exceptions.MethodProcessingException.UnknownMethodSourceException;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.github.GitHubClient;
import bio.terra.cbas.dependencies.github.GitHubService;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.util.methods.GithubUrlComponents;
import bio.terra.dockstore.model.ToolDescriptor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.EnumUtils;

import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.DOCKSTORE;
import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.GITHUB;

public final class MethodUtil {
  private static final String GITHUB_URL_HOST = "github.com";
  private static final String RAW_GITHUB_URL_HOST = "raw.githubusercontent.com";
  public static final List<String> SUPPORTED_URL_HOSTS =
      List.of(GITHUB_URL_HOST, RAW_GITHUB_URL_HOST);

  private MethodUtil() {}

  public static String convertGithubToRawUrl(String originalUrl)
      throws URISyntaxException, MalformedURLException {
    URL url = new URI(originalUrl).toURL();
    if (url.getHost().equals(RAW_GITHUB_URL_HOST)) {
      return originalUrl;
    } else {
      return originalUrl.replace(GITHUB_URL_HOST, RAW_GITHUB_URL_HOST).replace("/blob/", "/");
    }
  }

  public static String getRawUrl(
      MethodVersion methodVersion,
      MethodSourceEnum methodSource,
      GitHubService gitHubService,
      DockstoreService dockstoreService)
      throws URISyntaxException, MalformedURLException, bio.terra.dockstore.client.ApiException, GitHubClient.GitHubClientException, UnknownMethodSourceException {
    return switch (convertToMethodSourceEnum(methodVersion.method().methodSource())) {
      case GITHUB -> {
        GithubMethodDetails githubMethodDetails = methodVersion.method().githubMethodDetails().get();
        String originalUrl = gitHubService.getBaseUrl(githubMethodDetails.organization(), githubMethodDetails.repository(), methodVersion.branchOrTagName());
        yield convertGithubToRawUrl(originalUrl);
      }
      case DOCKSTORE -> {
        ToolDescriptor toolDescriptor =
            dockstoreService.descriptorGetV1(methodVersion.url(), methodVersion.name());
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

  public static GithubUrlComponents extractGithubDetailsFromUrl(String url)
      throws URISyntaxException {

    URI uri = new URI(url);
    String[] parts = uri.getPath().split("/");
    String[] gitHubPathParts = Arrays.stream(parts).skip(4).toArray(String[]::new);

    return new GithubUrlComponents(String.join("/", gitHubPathParts), parts[2], parts[1], parts[3]);
  }
}
