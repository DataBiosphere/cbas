package bio.terra.cbas.service;

import static bio.terra.cbas.common.MethodUtil.convertToMethodSourceEnum;
import static bio.terra.cbas.dependencies.github.GitHubService.getOrRebuildGithubUrl;

import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.dockstore.client.ApiException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MethodService {
  private final MethodDao methodDao;

  public MethodService(MethodDao methodDao) {
    this.methodDao = methodDao;
  }

  public void archiveMethod(UUID methodId) {
    methodDao.archiveMethod(methodId);
  }

  public static String getSubmissionUrl(
      MethodVersion methodVersion, DockstoreService dockstoreService)
      throws MethodProcessingException, ApiException, MalformedURLException, URISyntaxException {
    return switch (convertToMethodSourceEnum(methodVersion.method().methodSource())) {
      case DOCKSTORE -> {
        String resolvedMethodUrl =
            dockstoreService.descriptorGetV1(methodVersion.url(), methodVersion.name()).getUrl();
        if (resolvedMethodUrl == null || resolvedMethodUrl.isEmpty()) {
          throw new MethodProcessingException(
              "Error while retrieving WDL url for Dockstore workflow. No workflow url found specified path.");
        }
        yield resolvedMethodUrl;
      }
      case GITHUB -> getOrRebuildGithubUrl(methodVersion);
    };
  }
}
