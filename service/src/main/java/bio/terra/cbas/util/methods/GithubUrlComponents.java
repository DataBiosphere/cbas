package bio.terra.cbas.util.methods;

// TODO: this is from Katrina's PR - remove it once PR
// https://github.com/DataBiosphere/cbas/pull/242 merges
public record GithubUrlComponents(String path, String repo, String org, String branchOrTag) {}
