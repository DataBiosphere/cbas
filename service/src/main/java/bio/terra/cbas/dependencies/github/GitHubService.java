package bio.terra.cbas.dependencies.github;

import org.springframework.stereotype.Component;

@Component
public class GitHubService {
  private final GitHubClient client;

  public GitHubService(GitHubClient gitHubClient) {
    this.client = gitHubClient;
  }

  public Boolean isRepoPrivate(String organization, String repo)
      throws GitHubClient.GitHubClientException {
    GitHubClient.RepoInfo repoInfo = client.getRepo(organization, repo);
    return repoInfo.isPrivate();
  }
}
