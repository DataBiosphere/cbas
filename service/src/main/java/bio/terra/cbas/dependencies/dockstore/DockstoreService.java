package bio.terra.cbas.dependencies.dockstore;

import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.dockstore.api.Ga4Ghv1Api;
import bio.terra.dockstore.client.ApiException;
import bio.terra.dockstore.model.ToolDescriptor;
import org.springframework.stereotype.Component;

@Component
public class DockstoreService {
  private final DockstoreClient dockstoreClient;
  private static final String WDL_TYPE = "WDL";
  private static final String WORKFLOW_IDENTIFIER = "#workflow/";

  public DockstoreService(DockstoreClient dockstoreClient) {
    this.dockstoreClient = dockstoreClient;
  }

  private Ga4Ghv1Api ga4Ghv1Api() {
    return new Ga4Ghv1Api(dockstoreClient.getApiClient());
  }

  public ToolDescriptor descriptorGetV1(String workflowPath, String versionId) throws ApiException {
    return ga4Ghv1Api().descriptorGetV1(WDL_TYPE, WORKFLOW_IDENTIFIER + workflowPath, versionId);
  }

  public String resolveDockstoreUrl(MethodVersion methodVersion)
      throws MethodProcessingException, ApiException {
    String resolvedUrl = descriptorGetV1(methodVersion.url(), methodVersion.name()).getUrl();
    if (resolvedUrl == null || resolvedUrl.isEmpty()) {
      throw new MethodProcessingException(
          "Error while retrieving WDL url for Dockstore workflow. No workflow url found specified path.");
    }
    return resolvedUrl;
  }
}
