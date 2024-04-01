package bio.terra.cbas.dependencies.github;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestGithubClient {
  @Mock GitHubClient gitHubClient;
  @Mock Gson gson = new Gson();

  //  @Test
  //  void getsTargetGivenOrg() throws Exception {
  //    String org = "broad";
  //    String repo = "cromwell";
  //    Client client = mock(Client.class);
  //    WebTarget mockTarget = mock(WebTarget.class);
  //    Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
  //    GitHubClient gitHubClient = new GitHubClient();
  //
  //    GitHubClient.RepoInfo info = new GitHubClient.RepoInfo();
  //    info.url("www.iurl.com");
  //    info.isPrivate(false);
  //    info.id("abc123");
  //
  //    Response mockResponse =
  //        mockTarget
  //            .request(MediaType.APPLICATION_JSON_TYPE)
  //            .header("Accept", "application/vnd.github+json")
  //            .header("X-GitHub-Api-Version", "2022-11-28")
  //            .get();
  //
  //    // when(client.target("/test/me")).thenReturn(mockTarget);
  //
  //    when(mockResponse.getStatus()).thenReturn(200);
  //    assertFalse(gitHubClient.getRepo(org, repo, "").getIsPrivate());
  //    // GitHubClient.RepoInfo responseEntity =
  // mockResponse.readEntity(GitHubClient.RepoInfo.class);
  //  }

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
