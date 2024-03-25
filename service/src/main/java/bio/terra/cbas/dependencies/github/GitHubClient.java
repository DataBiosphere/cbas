package bio.terra.cbas.dependencies.github;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
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
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + token)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get();

    if (response.getStatus() == 200) {
      return gson.fromJson(response.readEntity(String.class), RepoInfo.class);
    } else {
      RepoError error = gson.fromJson(response.readEntity(String.class), RepoError.class);
      throw new GitHubClientException("GitHub Service getRepo failed: " + error.getMessage());
    }
  }

  public static class RepoInfo {
    // Adding this annotation because the "private" JSON field cannot be properly deserialized
    // being that it is a protected name in Java.
    @SerializedName("private")
    private Boolean isPrivate;

    private String url;
    private String id;

    public void isPrivate(Boolean isPrivate) {
      this.isPrivate = isPrivate;
    }

    public void url(String url) {
      this.url = url;
    }

    public void id(String id) {
      this.id = id;
    }

    public Boolean getIsPrivate() {
      return this.isPrivate;
    }

    public String getUrl() {
      return this.url;
    }

    public String getId() {
      return this.id;
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
