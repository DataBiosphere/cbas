package bio.terra.cbas.util.methods;

import org.springframework.stereotype.Component;

@Component
public class GithubUrlDetailsManager {

  public static GithubUrlComponents extractDetailsFromUrl(String url) {
    GithubUrlComponents githubUrlComponents = new GithubUrlComponents();

    // Get locations of slashes
    int firstSlash = url.indexOf("/");
    int secondSlash = url.indexOf("/", firstSlash + 1);
    int thirdSlash = url.indexOf("/", secondSlash + 1);
    int fourthSlash = url.indexOf("/", thirdSlash + 1);

    githubUrlComponents.setOrganization(url.substring(firstSlash + 1, secondSlash));
    githubUrlComponents.setRepository(url.substring(secondSlash + 1, thirdSlash));
    githubUrlComponents.setBranchOrTag(url.substring(thirdSlash + 1, fourthSlash));
    githubUrlComponents.setPath(url.substring(fourthSlash + 1));

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
