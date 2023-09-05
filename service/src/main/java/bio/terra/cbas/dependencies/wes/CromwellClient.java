package bio.terra.cbas.dependencies.wes;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.config.CromwellServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
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
  private final CredentialLoader credentialLoader;

  private final OkHttpClient singletonHttpClient;
  private final String cromwellUri;

  public CromwellClient(
      CromwellServerConfiguration cromwellServerConfiguration,
      CredentialLoader credentialLoader,
      String cromwellUri) {
    this.cromwellServerConfiguration = cromwellServerConfiguration;
    this.credentialLoader = credentialLoader;
    this.cromwellUri = cromwellUri;
    singletonHttpClient = new ApiClient().getHttpClient();
  }

  public ApiClient getWriteApiClient(String accessToken) {
    String uri;

    if (!cromwellServerConfiguration.fetchCromwellUrlFromLeo()) {
      uri = cromwellServerConfiguration.baseUri();
    } else {
      uri = cromwellUri;
    }
    ApiClient apiClient = new ApiClient().setBasePath(uri);
    apiClient.setAccessToken(accessToken);
    apiClient.setHttpClient(singletonHttpClient);
    // By closing the connection after each request, we avoid the problem of the open connection
    // being force-closed ungracefully by the Azure Relay/Listener infrastructure:
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(cromwellServerConfiguration.debugApiLogging());
    return apiClient;
  }

  public ApiClient getReadApiClient() throws AzureAccessTokenException {

    ApiClient apiClient = new ApiClient().setBasePath(cromwellServerConfiguration.baseUri());
    apiClient.setHttpClient(singletonHttpClient);
    // By closing the connection after each request, we avoid the problem of the open connection
    // being force-closed ungracefully by the Azure Relay/Listener infrastructure:
    apiClient.addDefaultHeader("Connection", "close");
    apiClient.setDebugging(cromwellServerConfiguration.debugApiLogging());
    return apiClient;
  }

  public ApiClient getReadApiClient(String accessToken) throws AzureAccessTokenException {
    ApiClient apiClient = getReadApiClient();
    apiClient.setAccessToken(accessToken);
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
