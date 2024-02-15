package bio.terra.cbas.util.methods;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class GithubUrlDetailsManager {

  public GithubUrlComponents extractDetailsFromUrl(String url) throws URISyntaxException {
    GithubUrlComponents githubUrlComponents = new GithubUrlComponents();

    URI uri = new URI(url);
    String[] parts = uri.getPath().split("/");
    String[] gitHubPathParts = Arrays.stream(parts).skip(4).toArray(String[]::new);

    githubUrlComponents.setOrganization(parts[1]);
    githubUrlComponents.setRepository(parts[2]);
    githubUrlComponents.setBranchOrTag(parts[3]);
    githubUrlComponents.setPath(String.join("/", gitHubPathParts));

    return githubUrlComponents;
  }

  public static class GithubUrlComponents {
    private String path = null;
    private String repository = null;
    private String organization = null;
    private String branchOrTag = null;

    public String getPath() {
      return path;
    }

    public GithubUrlComponents path(String path) {
      this.path = path;
      return this;
    }

    public GithubUrlComponents repository(String repository) {
      this.repository = repository;
      return this;
    }

    public GithubUrlComponents branchOrTag(String branchOrTag) {
      this.branchOrTag = branchOrTag;
      return this;
    }

    public GithubUrlComponents organization(String organization) {
      this.organization = organization;
      return this;
    }

    public String getRepository() {
      return repository;
    }

    public String getOrganization() {
      return organization;
    }

    public String getBranchOrTag() {
      return branchOrTag;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public void setRepository(String repo) {
      this.repository = repo;
    }

    public void setOrganization(String org) {
      this.organization = org;
    }

    public void setBranchOrTag(String name) {
      this.branchOrTag = name;
    }
  }
}
