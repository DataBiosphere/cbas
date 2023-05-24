package bio.terra.cbas.common;

import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum;
import bio.terra.dockstore.model.ToolDescriptor;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class MethodUtil {
  private static final String GITHUB_URL_HOST = "github.com";
  private static final String RAW_GITHUB_UTL_HOST = "raw.githubusercontent.com";
  public static final List<String> SUPPORTED_URL_HOSTS =
      new ArrayList<>(List.of(GITHUB_URL_HOST, RAW_GITHUB_UTL_HOST));

  public static String convertToRawGithubUrl(
      String originalUrl, MethodSourceEnum methodSource, String methodVersion, DockstoreService dockstoreService)
      throws URISyntaxException, MalformedURLException, UnsupportedEncodingException,
      bio.terra.dockstore.client.ApiException {
    return switch (methodSource) {
      case GITHUB -> {
        URL url = new URI(originalUrl).toURL();
        if (url.getHost().equals(RAW_GITHUB_UTL_HOST)) {
          yield originalUrl;
        } else {
          yield originalUrl.replace(GITHUB_URL_HOST, RAW_GITHUB_UTL_HOST).replace("/blob/", "/");
        }
      }
      case DOCKSTORE -> {
        ToolDescriptor toolDescriptor =
            dockstoreService.descriptorGetV1(originalUrl, methodVersion);
        yield toolDescriptor.getUrl();
      }
    };
  }
}
