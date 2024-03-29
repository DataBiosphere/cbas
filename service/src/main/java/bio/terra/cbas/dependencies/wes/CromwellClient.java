package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.DependencyUrlLoader;
import cromwell.client.ApiClient;
import cromwell.client.api.EngineApi;
import cromwell.client.api.Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi;
import cromwell.client.api.WomtoolApi;
import cromwell.client.api.WorkflowsApi;
import java.util.Optional;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

@Component
public class CromwellClient {

  private final CromwellServerConfiguration cromwellServerConfiguration;
  private final DependencyUrlLoader dependencyUrlLoader;

  private final OkHttpClient singletonHttpClient;

  public CromwellClient(
      CromwellServerConfiguration cromwellServerConfiguration,
      DependencyUrlLoader dependencyUrlLoader) {
    this.cromwellServerConfiguration = cromwellServerConfiguration;
    this.dependencyUrlLoader = dependencyUrlLoader;
    singletonHttpClient = new ApiClient().getHttpClient();
  }

  public ApiClient getWriteApiClient(String accessToken) throws DependencyNotAvailableException {
    String uri;

    if (!cromwellServerConfiguration.fetchCromwellUrlFromLeo()) {
      uri = cromwellServerConfiguration.baseUri();
    } else {
      uri =
          dependencyUrlLoader.loadDependencyUrl(
              DependencyUrlLoader.DependencyUrlType.CROMWELL_URL, accessToken);
    }
    ApiClient apiClient = setupApiClient(new ApiClient());
    apiClient.setBasePath(uri);
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }

  public ApiClient getReadApiClient() {
    ApiClient apiClient = setupApiClient(new ApiClient());
    apiClient.setBasePath(cromwellServerConfiguration.baseUri());
    return apiClient;
  }

  public ApiClient getAuthReadApiClient(String accessToken) {

    ApiClient apiClient = setupApiClient(new ApiClient());
    apiClient.setBasePath(cromwellServerConfiguration.baseUri());
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }

  public ApiClient setupApiClient(ApiClient apiClient) {
    apiClient.setHttpClient(singletonHttpClient);
    // By closing the connection after each request, we avoid the problem of the open connection
    // being force-closed ungracefully by the Azure Relay/Listener infrastructure:
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(cromwellServerConfiguration.debugApiLogging());
    return apiClient;
  }

  public Optional<String> getFinalWorkflowLogDirOption() {
    return Optional.ofNullable(this.cromwellServerConfiguration.finalWorkflowLogDir());
  }

  public Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi wesAPI(ApiClient apiClient) {
    return new Ga4GhWorkflowExecutionServiceWesAlphaPreviewApi(apiClient);
  }

  public EngineApi engineApi(ApiClient apiClient) {
    return new EngineApi(apiClient);
  }

  public WorkflowsApi workflowsApi(ApiClient apiClient) {
    return new WorkflowsApi(apiClient);
  }

  public WomtoolApi womtoolApi(ApiClient apiClient) {
    return new WomtoolApi(apiClient);
  }
}
