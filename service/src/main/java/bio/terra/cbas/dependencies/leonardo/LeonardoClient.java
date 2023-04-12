package bio.terra.cbas.dependencies.leonardo;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.dependencies.common.CredentialLoader;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class LeonardoClient {

  private final LeonardoServerConfiguration leonardoServerConfiguration;
  private final CredentialLoader credentialLoader;

  public LeonardoClient(
      LeonardoServerConfiguration leonardoServerConfiguration, CredentialLoader credentialLoader) {
    this.leonardoServerConfiguration = leonardoServerConfiguration;
    this.credentialLoader = credentialLoader;
  }

  public ApiClient getApiClient() throws AzureAccessTokenException {
    return new ApiClient()
        .setBasePath(leonardoServerConfiguration.baseUri())
        .addDefaultHeader(
            "Authorization",
            "Bearer %s"
                .formatted(
                    credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN)));
  }
}
