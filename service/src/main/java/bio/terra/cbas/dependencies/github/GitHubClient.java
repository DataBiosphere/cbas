package bio.terra.cbas.dependencies.github;

import com.google.gson.Gson;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

  public RepoInfo getRepo(String organization, String repo, String token)
      throws GitHubClientException {
    WebTarget target = client.target(BASE_URL).path("/repos").path(organization).path(repo);
    Response response =
        target
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "Bearer: " + token)
            .get();
    if (response.getStatus() == 200) {
      return gson.fromJson(response.readEntity(String.class), RepoInfo.class);
    } else {
      RepoError error = gson.fromJson(response.readEntity(String.class), RepoError.class);
      throw new GitHubClientException("GitHub Service getRepo failed: " + error.getMessage());
    }
  }

  public static class RepoInfo {
    private boolean isPrivate;
    private String url;
    private String id;

    public boolean isPrivate() {
      return isPrivate;
    }

    public String getUrl() {
      return url;
    }

    public String getId() {
      return id;
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
