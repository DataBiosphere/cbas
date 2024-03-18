package bio.terra.cbas.dependencies.github;

import com.google.gson.Gson;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class GitHubClient {
  private static final String BASE_URL = "https://api.github.com";
  private final Client client;
  private final Gson gson;

  public GitHubClient() {
    this.client = ClientBuilder.newClient();
    this.gson = new Gson();
  }

  public JSONObject getRepo(String organization, String repo, String token)
      throws GitHubClientException {

    WebTarget target = client.target(BASE_URL).path("/repos").path(organization).path(repo);
    Response response =
        target
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + token)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get();

    if (response.getStatus() == 200) {
      String gitHubResponse = response.readEntity(String.class);
      return new JSONObject(gitHubResponse);
    } else {
      RepoError error = gson.fromJson(response.readEntity(String.class), RepoError.class);
      throw new GitHubClientException("GitHub Service getRepo failed: " + error.getMessage());
    }
  }

  public static class RepoError {
    private String message;

    public String getMessage() {
      return message;
    }
  }

  public class GitHubClientException extends Exception {
    public GitHubClientException(String message) {
      super(message);
    }
  }
}
