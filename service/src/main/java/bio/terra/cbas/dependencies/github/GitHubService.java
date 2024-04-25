package bio.terra.cbas.dependencies.github;

import bio.terra.cbas.dependencies.ecm.EcmService;
import bio.terra.common.iam.BearerToken;
import org.springframework.stereotype.Component;

@Component
public class GitHubService {
  private final GitHubClient client;
  private final EcmService ecmService;

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

  public String getCurrentGithash(String organization, String repo, String branch)
      throws GitHubClient.GitHubClientException {
    GitHubClient.CommitInfo commitInfo;
    try {
      commitInfo = client.getCommit(organization, repo, branch, "");
    } catch (GitHubClient.GitHubClientException e) {
      String githubToken = ecmService.getAccessToken();
      commitInfo = client.getCommit(organization, repo, branch, githubToken);
    }

    return commitInfo.getSha();
  }
}
