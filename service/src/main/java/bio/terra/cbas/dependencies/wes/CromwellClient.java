package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
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
  private final CredentialLoader credentialLoader;

  private final OkHttpClient singletonHttpClient;

  public CromwellClient(
      CromwellServerConfiguration cromwellServerConfiguration,
      DependencyUrlLoader dependencyUrlLoader,
      CredentialLoader credentialLoader) {
    this.cromwellServerConfiguration = cromwellServerConfiguration;
    this.dependencyUrlLoader = dependencyUrlLoader;
    this.credentialLoader = credentialLoader;
    singletonHttpClient = new ApiClient().getHttpClient();
  }

  public ApiClient getWriteApiClient()
      throws DependencyNotAvailableException, AzureAccessTokenException {
    String uri =
        dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.CROMWELL_URL);

    ApiClient apiClient = new ApiClient().setBasePath(uri);
    apiClient.setHttpClient(singletonHttpClient);
    apiClient.addDefaultHeader(
        "Authorization",
        "Bearer " + credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    // By closing the connection after each request, we avoid the problem of the open connection
    // being force-closed ungracefully by the Azure Relay/Listener infrastructure:
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(cromwellServerConfiguration.debugApiLogging());
    return apiClient;
  }

  public ApiClient getReadApiClient() throws AzureAccessTokenException {

    ApiClient apiClient = new ApiClient().setBasePath(cromwellServerConfiguration.baseUri());
    apiClient.setHttpClient(singletonHttpClient);
    apiClient.addDefaultHeader(
        "Authorization",
        "Bearer " + credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
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
