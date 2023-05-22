package bio.terra.cbas.common;

import bio.terra.cbas.common.exceptions.DockstoreDescriptorException;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.models.DockstoreWdlDescriptor;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public final class MethodUtil {

  // private final RestTemplate restTemplate = new RestTemplateBuilder().build();

  public static final String GITHUB_URL_HOST = "github.com";
  public static final String RAW_GITHUB_UTL_HOST = "raw.githubusercontent.com";
  public static final List<String> SUPPORTED_URL_HOSTS =
      new ArrayList<>(List.of(GITHUB_URL_HOST, RAW_GITHUB_UTL_HOST));

  public static String convertToRawGithubUrl(
      String originalUrl, PostMethodRequest.MethodSourceEnum methodSource, String methodVersion)
      throws URISyntaxException, MalformedURLException, UnsupportedEncodingException,
          DockstoreDescriptorException.DockstoreDescriptorNotFoundException {
    return switch (methodSource) {
      case GITHUB -> {
        URL url = new URI(originalUrl).toURL();
        if (url.getHost().equals(RAW_GITHUB_UTL_HOST)) {
          yield originalUrl;
        } else {
          yield originalUrl.replace(GITHUB_URL_HOST, RAW_GITHUB_UTL_HOST).replace("/blob/", "/");
        }
      }
      case DOCKSTORE -> fetchRawUrlFromDockstore(originalUrl, methodVersion);
    };
  }

  public static String fetchRawUrlFromDockstore(String methodPath, String methodVersion)
      throws DockstoreDescriptorException.DockstoreDescriptorNotFoundException {
    String encodedVersion = URLEncoder.encode(methodVersion, StandardCharsets.UTF_8);
    String encodedUrlPath = URLEncoder.encode(methodPath, StandardCharsets.UTF_8);
    String workflowVersionsPath =
        "api/ga4gh/v1/tools/%23workflow%2F" + encodedUrlPath + "/versions";
    String wdlPath = workflowVersionsPath + "/" + encodedVersion + "/WDL/descriptor";

    // TODO: Saloni - move this to config
    String dockstoreRootUrl = "https://staging.dockstore.org";

    String requestUrl = dockstoreRootUrl + "/api/" + wdlPath;
    //    DockstoreWdlDescriptor descriptorResponse = restTemplate.getForObject(requestUrl,
    // DockstoreWdlDescriptor.class);

    Client commonHttpClient = ClientBuilder.newClient();
    DockstoreWdlDescriptor descriptorResponse =
        commonHttpClient.target(requestUrl).request().get(DockstoreWdlDescriptor.class);

    commonHttpClient.close();

    if (descriptorResponse != null) {

      System.out.printf("RESPONSE descriptor: %s%n", descriptorResponse.getDescriptor());
      System.out.printf("RESPONSE type: %s%n", descriptorResponse.getType());
      System.out.printf("RESPONSE url: %s%n", descriptorResponse.getUrl());

      return descriptorResponse.getUrl();
    } else {
      throw new DockstoreDescriptorException.DockstoreDescriptorNotFoundException(methodPath);
    }
  }
}
