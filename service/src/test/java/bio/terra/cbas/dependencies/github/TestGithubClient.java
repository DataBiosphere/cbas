package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
  void getsTargetGivenOrg() throws Exception {
    String org = "broad";
    String repo = "cromwell";
    WebTarget mockTarget = mock(WebTarget.class);
    Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
    Response mockResponse = mock(Response.class);

    when(mockResponse.getStatus()).thenReturn(200);
    when(mockBuilder.get()).thenReturn(mockResponse);
    when(mockTarget.path(any())).thenReturn(mockTarget);
    when(mockTarget.request(MediaType.APPLICATION_JSON_TYPE)).thenReturn(mockBuilder);

    when(client.target(any(String.class))).thenReturn(mockTarget);

    GitHubClient.RepoInfo info = new GitHubClient.RepoInfo();
    info.url("www.iurl.com");
    info.isPrivate(false);
    info.id("abc123");

    when(mockTarget.request(MediaType.APPLICATION_JSON_TYPE).headers(any()))
        .thenReturn(mockBuilder);
    when(gson.fromJson(mockResponse.readEntity(String.class), GitHubClient.RepoInfo.class))
        .thenReturn(info);

    GitHubClient gc = new GitHubClient(client, gson);
    assertFalse(gc.getRepo(org, repo, "").getIsPrivate());
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
}
