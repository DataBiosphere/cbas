package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.validation.CbasValidVoid;
import bio.terra.cbas.common.validation.CbasValidationError;
import bio.terra.cbas.common.validation.CbasVoidValidation;
import bio.terra.cbas.dependencies.ecm.EcmService;
import bio.terra.common.iam.BearerToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
        "https://raw.githubusercontent.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
        "https://github.com/broadinstitute/cromwell/blob/develop/forkjoin.wdl",
        "https://raw.githubusercontent.com/broadinstitute/cromwell/blob/develop/forkjoin.wdl",
      })
  void testValidateGithubUrlGood(String url) {
    CbasValidVoid expected = CbasValidVoid.INSTANCE;
    CbasVoidValidation actual = GitHubService.validateGithubUrl(url);

    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://fake-github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
        "https://raw.githubusercontent.fake.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
        "https://www.github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
      })
  void testValidateGithubUrlBadHost(String url) {
    CbasVoidValidation expected =
        CbasValidationError.of(
            "method_url is invalid. Supported URI host(s): [github.com, raw.githubusercontent.com]");
    CbasVoidValidation actual = GitHubService.validateGithubUrl(url);

    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
        "raw.githubusercontent.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
      })
  void testValidateGithubUrlNotAbsolute(String url) {
    CbasVoidValidation expected =
        CbasValidationError.of("method_url is invalid. Reason: URI is not absolute");
    CbasVoidValidation actual = GitHubService.validateGithubUrl(url);

    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://github.com/broadinstitute/cromwell/forkjoin.wdl",
        "https://github.com/broadinstitute/",
        "https://github.com/broadinstitute/cromwell/blob/forkjoin.wdl",
      })
  void testValidateGithubUrlBadFormat(String url) {
    CbasVoidValidation expected =
        CbasValidationError.of(
            "method_url is invalid. Github URL should be formatted like: <hostname> / <org> / <repo> / blob / <branch/tag/commit> / <path-to-file>");
    CbasVoidValidation actual = GitHubService.validateGithubUrl(url);

    assertEquals(expected, actual);
  }
}
