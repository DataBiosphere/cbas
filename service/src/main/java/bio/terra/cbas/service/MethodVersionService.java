package bio.terra.cbas.service;

import static bio.terra.cbas.common.MethodUtil.asRawMethodUrlGithub;
import static bio.terra.cbas.common.MethodUtil.convertToMethodSourceEnum;
import static bio.terra.cbas.dependencies.github.GitHubService.buildRawGithubUrl;
import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.DOCKSTORE;
import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.GITHUB;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.model.MethodVersionDetails;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.dockstore.client.ApiException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MethodVersionService {

  private final DockstoreService dockstoreService;

  public MethodVersionService(DockstoreService dockstoreService) {
    this.dockstoreService = dockstoreService;
  }

  public static String getSubmissionUrl(
      MethodVersion methodVersion, DockstoreService dockstoreService)
      throws MethodProcessingException, ApiException, MalformedURLException, URISyntaxException {
    return switch (convertToMethodSourceEnum(methodVersion.method().methodSource())) {
      case DOCKSTORE -> dockstoreService.resolveDockstoreUrl(methodVersion);
      case GITHUB -> getOrRebuildGithubUrl(methodVersion);
    };
  }

  public MethodVersionDetails methodVersionToMethodVersionDetails(MethodVersion methodVersion)
      throws MalformedURLException, URISyntaxException, MethodProcessingException,
          bio.terra.dockstore.client.ApiException {
    String resolvedUrl;
    if (Objects.equals(methodVersion.method().methodSource(), DOCKSTORE.toString())) {
      resolvedUrl = dockstoreService.resolveDockstoreUrl(methodVersion);
    } else if (Objects.equals(methodVersion.method().methodSource(), GITHUB.toString())) {
      resolvedUrl = getOrRebuildGithubUrl(methodVersion);
    } else {
      resolvedUrl = methodVersion.url();
    }

    return new MethodVersionDetails()
        .methodVersionId(methodVersion.methodVersionId())
        .methodId(methodVersion.method().methodId())
        .name(methodVersion.name())
        .description(methodVersion.description())
        .created(DateUtils.convertToDate(methodVersion.created()))
        .lastRun(MethodService.initializeLastRunDetails(methodVersion.lastRunSetId()))
        .branchOrTagName(methodVersion.branchOrTagName())
        .url(resolvedUrl);
  }

  private static String getOrRebuildGithubUrl(MethodVersion methodVersion)
      throws MalformedURLException, URISyntaxException {
    Optional<GithubMethodVersionDetails> methodVersionDetailsOptional =
        methodVersion.methodVersionDetails();
    Optional<GithubMethodDetails> methodDetailsOptional =
        methodVersion.method().githubMethodDetails();

    if (methodVersionDetailsOptional.isEmpty() || methodDetailsOptional.isEmpty()) {
      return asRawMethodUrlGithub(methodVersion.url());
    } else {
      GithubMethodDetails methodDetails = methodDetailsOptional.get();
      GithubMethodVersionDetails methodVersionDetails = methodVersionDetailsOptional.get();
      return buildRawGithubUrl(
          methodDetails.organization(),
          methodDetails.repository(),
          methodVersionDetails.githash(),
          methodDetails.path());
    }
  }
}
