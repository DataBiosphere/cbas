package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dependencies.github.GitHubClient.GitHubClientException;
import bio.terra.cbas.dependencies.github.GitHubClient.RepoError;
import bio.terra.cbas.dependencies.github.GitHubClient.RepoInfo;
import com.google.gson.Gson;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestGithubClient {
  @Mock Gson gson;
  @Mock Client client;

  @Test
  void getsRepoInfoGiven200() throws Exception {
    String org = "broad";
    String repo = "cromwell";

    RepoInfo info = new GitHubClient.RepoInfo();
    info.url("www.myurl.com");
    info.isPrivate(false);
    info.id("abc123");

    buildMocks(200, info, null);

    GitHubClient gc = new GitHubClient(client, gson);
    assertFalse(gc.getRepo(org, repo, "").getIsPrivate());
    assertEquals("www.myurl.com", gc.getRepo(org, repo, "").getUrl());
    assertEquals("abc123", gc.getRepo(org, repo, "").getId());
  }

  @Test
  void throwsExpectedMessageFor4xx() {
    String org = "broad";
    String repo = "cromwell";
    RepoError error = new RepoError();
    error.message("thrown message");

    buildMocks(404, null, error);

    GitHubClient gc = new GitHubClient(client, gson);
    GitHubClientException exception =
        assertThrows(GitHubClientException.class, () -> gc.getRepo(org, repo, ""));
    assertEquals("GitHub Service getRepo failed: thrown message", exception.getMessage());
  }

  @Test
  void getsCorrectNumberOfHeadersWithEmptyToken() {
    String emptyToken = "";
    GitHubClient client = new GitHubClient();

    MultivaluedMap<String, Object> actualMap = client.getHeaders(emptyToken);

    assertEquals(2, actualMap.size());
  }

  @Test
  void getsCorrectNumberOfHeadersWithToken() {
    String token = "foobar";
    GitHubClient client = new GitHubClient();

    MultivaluedMap<String, Object> actualMap = client.getHeaders(token);

    assertEquals(3, actualMap.size());
  }

  // helper method
  void buildMocks(int returnStatus, RepoInfo info, RepoError error) {
    WebTarget mockTarget = mock(WebTarget.class);
    Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
    Response mockResponse = mock(Response.class);

    when(mockResponse.getStatus()).thenReturn(returnStatus);
    when(mockBuilder.get()).thenReturn(mockResponse);
    when(mockTarget.path(any())).thenReturn(mockTarget);
    when(mockTarget.request(MediaType.APPLICATION_JSON_TYPE)).thenReturn(mockBuilder);

    when(client.target(any(String.class))).thenReturn(mockTarget);

    when(mockTarget.request(MediaType.APPLICATION_JSON_TYPE).headers(any()))
        .thenReturn(mockBuilder);

    if (returnStatus == 200) {
      when(gson.fromJson(mockResponse.readEntity(String.class), RepoInfo.class)).thenReturn(info);
    } else {
      when(gson.fromJson(mockResponse.readEntity(String.class), RepoError.class)).thenReturn(error);
    }
  }
}
