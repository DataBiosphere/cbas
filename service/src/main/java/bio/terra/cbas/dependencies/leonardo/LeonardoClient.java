package bio.terra.cbas.dependencies.leonardo;

import bio.terra.cbas.config.LeonardoServerConfiguration;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiClient;
import org.springframework.stereotype.Component;

@Component
public class LeonardoClient {

  private final LeonardoServerConfiguration leonardoServerConfiguration;

  public LeonardoClient(LeonardoServerConfiguration leonardoServerConfiguration) {
    this.leonardoServerConfiguration = leonardoServerConfiguration;
  }

  public ApiClient getApiClient() {
    return new ApiClient().setBasePath(leonardoServerConfiguration.baseUri());
  }
}
