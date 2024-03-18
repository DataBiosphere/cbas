package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestGithubService {

  @Test
  void returnCorrectBooleanValue() throws GitHubClient.GitHubClientException {
    String jsonResponseFalse =
        """
        {
          "name": "foo",
          "organization": "broadinstitute",
          "private": %s
        }
        """
            .formatted("false")
            .stripIndent()
            .trim();

    String jsonResponseTrue =
        """
        {
          "name": "foo",
          "organization": "broadinstitute",
          "private": %s
        }
        """
            .formatted("true")
            .stripIndent()
            .trim();
    JSONObject jsonObjectFalse = new JSONObject(jsonResponseFalse);
    JSONObject jsonObjectTrue = new JSONObject(jsonResponseTrue);

    GitHubClient githubClient = mock(GitHubClient.class);
    GitHubService gitHubService = new GitHubService(githubClient);
    when(githubClient.getRepo("broadinstitute", "foo", "token")).thenReturn(jsonObjectFalse);
    assertFalse(gitHubService.isRepoPrivate("broadinstitute", "foo", "token"));

    when(githubClient.getRepo("broadinstitute", "foo", "token")).thenReturn(jsonObjectTrue);
    assertTrue(gitHubService.isRepoPrivate("broadinstitute", "foo", "token"));
  }
}
