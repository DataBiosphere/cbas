package bio.terra.cbas.dependencies.github;

import bio.terra.cbas.config.GitHubConfiguration;
import com.google.gson.Gson;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.stereotype.Component;

@Component
public class GitHubClient {
  private static final String BASE_URL = "https://api.github.com";
  private final Client client;
  private final String token;
  private final Gson gson;

  public GitHubClient(GitHubConfiguration gitHubConfiguration) {
    this.client = ClientBuilder.newClient();
    this.token = gitHubConfiguration.personalAccessToken();
    this.gson = new Gson();
  }

  public RepoInfo getRepo(String organization, String repo) throws GitHubClientException {
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
