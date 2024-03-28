package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestGithubService {

  @Test
  void returnCorrectBooleanValue() throws GitHubClient.GitHubClientException {

    GitHubClient githubClient = mock(GitHubClient.class);
    GitHubService gitHubService = new GitHubService(githubClient);
    GitHubClient.RepoInfo repoInfo = new GitHubClient.RepoInfo();
    repoInfo.id("random id");
    repoInfo.url("www.url.com");
    repoInfo.isPrivate(true);

    when(githubClient.getRepo("broadinstitute", "foo", "token")).thenReturn(repoInfo);
    assertTrue(gitHubService.isRepoPrivate("broadinstitute", "foo", "token"));
  }

  @Test
  void throwsClientError() throws GitHubClient.GitHubClientException {

    GitHubClient githubClient = mock(GitHubClient.class);
    GitHubService gitHubService = new GitHubService(githubClient);

    when(githubClient.getRepo("broadinstitute", "foo", "token"))
        .thenThrow(GitHubClient.GitHubClientException.class);
    assertThrows(
        GitHubClient.GitHubClientException.class,
        () -> gitHubService.isRepoPrivate("broadinstitute", "foo", "token"));
  }
}
