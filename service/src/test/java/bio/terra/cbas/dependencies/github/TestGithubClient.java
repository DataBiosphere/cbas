package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.gson.Gson;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestGithubClient {
  private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
      new com.fasterxml.jackson.databind.ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  @Mock GitHubClient gitHubClientMock;
  @Mock Gson gson = new Gson();

  @Test
  void getsTargetGivenOrg() throws Exception {
    String org = "broad";
    String repo = "cromwell";
    Client client = mock(Client.class);
    WebTarget mockTarget = mock(WebTarget.class);
    Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
    GitHubClient gitHubClient = new GitHubClient();
    MultivaluedMap<String, Object> headerMap = new MultivaluedHashMap<>();
    Response resp = mock(Response.class);
    String response =
        """
        {
          "id": "abc123",
          "private": "false",
          "url": "www.urlfoo.com"
        }
        """
            .trim();

    // Response newResponse = objectMapper.readValue(response, Response.class);

    // when(client.target("/test/target")).thenReturn(mockTarget);
    // when(gitHubClientMock.getHeaders("")).thenReturn(headerMap);
    when(mockTarget.request(MediaType.APPLICATION_JSON_TYPE)).thenReturn(mockBuilder);
    // when(mockTarget.request(MediaType.APPLICATION_JSON_TYPE).get()).thenReturn(resp);
    // when(resp.getStatus()).thenReturn(200);
    // when(resp.readEntity(String.class)).thenReturn(response);

    GitHubClient.RepoInfo info = new GitHubClient.RepoInfo();
    info.url("www.iurl.com");
    info.isPrivate(false);
    info.id("abc123");
    //    when(mockTarget
    //            .request(MediaType.APPLICATION_JSON_TYPE)
    //            .header("header", "one")
    //            .header("header", "two"))
    //        .thenReturn(mockBuilder);

    //    Response mockResponse =
    //        mockTarget
    //            .request(MediaType.APPLICATION_JSON_TYPE)
    //            .header("Accept", "application/vnd.github+json")
    //            .header("X-GitHub-Api-Version", "2022-11-28")
    //            .get();

    // when(client.target("/test/me")).thenReturn(mockTarget);

    // GitHubClient.RepoInfo repoInfo = gitHubClientMock.getRepo(org, repo, "");
    // when(gitHubClientMock.getRepo(org, repo, "")).thenReturn(info);
    assertFalse(info.getIsPrivate());
    // GitHubClient.RepoInfo responseEntity =
    // mockResponse.readEntity(GitHubClient.RepoInfo.class);
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

//  // when(client.target("/test/me").path("/repos").path(org).path(repo)).thenReturn(mockTarget);
//  when(mockTarget.request(MediaType.APPLICATION_JSON_TYPE)).thenReturn(mockBuilder);
//
//    Response mockResponse =
//    mockTarget
//    .request(MediaType.APPLICATION_JSON_TYPE)
//    .header("Accept", "application/vnd.github+json")
//    .header("X-GitHub-Api-Version", "2022-11-28")
//    .get();
//
//    //    when(mockBuilder.header("Content-type", MediaType.APPLICATION_JSON_TYPE))
//    //        .thenReturn(mockBuilder);
//    when(mockBuilder.get()).thenReturn(mockResponse);
//
//    when(gson.fromJson(mockResponse.readEntity(String.class), GitHubClient.RepoInfo.class))
//    .thenReturn(info);