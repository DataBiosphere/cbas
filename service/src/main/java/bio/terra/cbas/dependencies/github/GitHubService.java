package bio.terra.cbas.dependencies.github;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class GitHubService {
  private final GitHubClient client;

  public GitHubService(GitHubClient gitHubClient) {
    this.client = gitHubClient;
  }

  public Boolean isRepoPrivate(String organization, String repo, String token)
      throws GitHubClient.GitHubClientException {
    JSONObject repoInfo = client.getRepo(organization, repo, token);
    return Boolean.valueOf(repoInfo.get("private").toString());
  }
}
