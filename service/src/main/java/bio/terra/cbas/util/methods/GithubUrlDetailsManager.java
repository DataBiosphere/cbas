package bio.terra.cbas.util.methods;

public class GithubUrlDetailsManager {

  public record GithubUrlComponents() {
    private static String path = null;
    private static String repository = null;
    private static String organization = null;
    private static String branchOrTag = null;

    public String getPath() {
      return path;
    }

    public GithubUrlComponents path(String path) {
      GithubUrlComponents.path = path;
      return this;
    }

    public GithubUrlComponents repository(String repository) {
      GithubUrlComponents.repository = repository;
      return this;
    }

    public GithubUrlComponents branchOrTag(String branchOrTag) {
      GithubUrlComponents.branchOrTag = branchOrTag;
      return this;
    }

    public GithubUrlComponents organization(String organization) {
      GithubUrlComponents.organization = organization;
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
      GithubUrlComponents.path = path;
    }

    public void setRepository(String repo) {
      repository = repo;
    }

    public void setOrganization(String org) {
      organization = org;
    }

    public void setBranchOrTag(String name) {
      branchOrTag = name;
    }
  }
}
