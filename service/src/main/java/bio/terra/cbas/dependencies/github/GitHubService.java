package bio.terra.cbas.dependencies.github;

import static bio.terra.cbas.common.MethodUtil.asRawMethodUrlGithub;

import bio.terra.cbas.common.validation.CbasValidationError;
import bio.terra.cbas.common.validation.CbasValidVoid;
import bio.terra.cbas.common.validation.CbasVoidValidation;
import bio.terra.cbas.dependencies.ecm.EcmService;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.common.iam.BearerToken;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class GitHubService {
  private final GitHubClient client;
  private final EcmService ecmService;

  public static final String GITHUB_URL_HOST = "github.com";
  public static final String RAW_GITHUB_URL_HOST = "raw.githubusercontent.com";
  public static final List<String> SUPPORTED_GITHUB_HOSTS =
      List.of(GITHUB_URL_HOST, RAW_GITHUB_URL_HOST);

  public GitHubService(GitHubClient gitHubClient, EcmService ecmService) {
    this.client = gitHubClient;
    this.ecmService = ecmService;
  }

  public Boolean isRepoPrivate(String organization, String repo, BearerToken userToken)
      throws GitHubClient.GitHubClientException {
    GitHubClient.RepoInfo repoInfo;
    try {
      repoInfo = client.getRepo(organization, repo, "");
    } catch (GitHubClient.GitHubClientException e) {
      String githubToken = ecmService.getAccessToken(userToken);
      repoInfo = client.getRepo(organization, repo, githubToken);
    }

    return repoInfo.getIsPrivate();
  }

  public String getCurrentGithash(
      String organization, String repo, String branch, BearerToken userToken)
      throws GitHubClient.GitHubClientException {
    GitHubClient.CommitInfo commitInfo;
    try {
      commitInfo = client.getCommit(organization, repo, branch, "");
    } catch (GitHubClient.GitHubClientException e) {
      String githubToken = ecmService.getAccessToken(userToken);
      commitInfo = client.getCommit(organization, repo, branch, githubToken);
    }

    return commitInfo.getSha();
  }

  // If the methodVersion has methodVersionDetails, reconstruct the url using them
  public static String getOrRebuildGithubUrl(MethodVersion methodVersion)
      throws MalformedURLException, URISyntaxException {
    Optional<GithubMethodVersionDetails> methodVersionDetailsOptional =
        methodVersion.methodVersionDetails();
    Optional<GithubMethodDetails> methodDetailsOptional =
        methodVersion.method().githubMethodDetails();

    if (methodVersionDetailsOptional.isEmpty() || methodDetailsOptional.isEmpty()) {
      return asRawMethodUrlGithub(methodVersion.url());
    } else {
      GithubMethodDetails methodDetails = methodDetailsOptional.get();
      GithubMethodVersionDetails methodVersionDetails = methodVersionDetailsOptional.get();
      return buildRawGithubUrl(
          methodDetails.organization(),
          methodDetails.repository(),
          methodVersionDetails.githash(),
          methodDetails.path());
    }
  }

  public static String buildRawGithubUrl(String org, String repo, String githash, String path) {
    return "https://%s/%s/%s/%s/%s".formatted(RAW_GITHUB_URL_HOST, org, repo, githash, path);
  }

  // helper method to verify that URL is valid and its host is supported
  public static CbasVoidValidation validateGithubUrl(String methodUrl) {
    try {
      URL url = new URI(methodUrl).toURL();

      List<String> pathElements =
          Arrays.stream(url.getPath().split("/")).filter(Predicate.not(String::isEmpty)).toList();

      if (!SUPPORTED_GITHUB_HOSTS.contains(url.getHost())) {
        return new CbasValidationError("method_url is invalid. Supported URI host(s): " + SUPPORTED_GITHUB_HOSTS);
      } else if (pathElements.size() < 5) {
        return new CbasValidationError("method_url is invalid. Github URL should be formatted like: <hostname> / <org> / <repo> / blob / <branch/tag/commit> / <path-to-file>");
      }
    } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
      return new CbasValidationError("method_url is invalid. Reason: " + e.getMessage());
    }

    return CbasValidVoid.INSTANCE;
  }
}
