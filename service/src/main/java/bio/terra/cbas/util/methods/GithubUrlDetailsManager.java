package bio.terra.cbas.util.methods;

public class GithubUrlDetailsManager {

  public record GithubUrlComponents(String path, String repo, String org, String branchOrTag) {}
}
