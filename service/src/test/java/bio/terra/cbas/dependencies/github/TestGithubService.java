package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dependencies.ecm.EcmService;
import bio.terra.common.iam.BearerToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class TestGithubService {
  private final BearerToken mockUserToken = new BearerToken("mock-token");

  @Test
  void returnCorrectBooleanValueNoToken() throws GitHubClient.GitHubClientException {

    GitHubClient githubClient = mock(GitHubClient.class);
    EcmService ecmService = mock(EcmService.class);
    GitHubService gitHubService = new GitHubService(githubClient, ecmService);
    GitHubClient.RepoInfo repoInfo = new GitHubClient.RepoInfo();
    repoInfo.id("random id");
    repoInfo.url("www.url.com");
    repoInfo.isPrivate(false);

    when(githubClient.getRepo("broadinstitute", "foo", "")).thenReturn(repoInfo);
    assertFalse(gitHubService.isRepoPrivate("broadinstitute", "foo", mockUserToken));
  }

  @Test
  void getsPrivateGivenToken() throws GitHubClient.GitHubClientException {

    GitHubClient githubClient = mock(GitHubClient.class);
    EcmService ecmService = mock(EcmService.class);
    GitHubService gitHubService = new GitHubService(githubClient, ecmService);
    GitHubClient.RepoInfo repoInfo = new GitHubClient.RepoInfo();
    repoInfo.id("random id");
    repoInfo.url("www.url.com");
    repoInfo.isPrivate(true);

    when(githubClient.getRepo("broadinstitute", "foo", ""))
        .thenThrow(GitHubClient.GitHubClientException.class);
    when(ecmService.getAccessToken(mockUserToken)).thenReturn("token");
    when(githubClient.getRepo("broadinstitute", "foo", "token")).thenReturn(repoInfo);

    assertTrue(gitHubService.isRepoPrivate("broadinstitute", "foo", mockUserToken));
  }

  @Test
  void throwsErrorIfNoTokenForPrivateMethod() throws GitHubClient.GitHubClientException {
    GitHubClient githubClient = mock(GitHubClient.class);
    EcmService ecmService = mock(EcmService.class);
    GitHubService gitHubService = new GitHubService(githubClient, ecmService);

    when(githubClient.getRepo("broadinstitute", "foo", ""))
        .thenThrow(GitHubClient.GitHubClientException.class);
    when(ecmService.getAccessToken(mockUserToken)).thenThrow(new RestClientException("exception"));

    assertThrows(
        RestClientException.class,
        () -> gitHubService.isRepoPrivate("broadinstitute", "foo", mockUserToken));
  }
}
