package scripts.client;

import bio.terra.cbas.client.ApiClient;
import bio.terra.testrunner.runner.config.ServerSpecification;
import java.io.IOException;

public class BatchClient extends ApiClient {

  /**
   * Build the no-auth API client object for the CBAS server. No access token is needed for this API
   * client.
   *
   * @param server the server specification. Currently ignored and tests actually against localhost
   *     (in github, a test service created in a previous action step).
   */
  public BatchClient(ServerSpecification server) throws IOException {
    // TODO: Let this float? Set this up automatically? (Right now the github action which triggers
    // the test pre-creates a CBAS service to test against)
    setBasePath("http://localhost:8080");
  }
}
