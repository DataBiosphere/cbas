package bio.terra.cbas.pact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import bio.terra.externalcreds.pact.State;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import bio.terra.cbas.config.EcmServerConfiguration;
import bio.terra.cbas.dependencies.ecm.EcmClient;
import bio.terra.cbas.dependencies.ecm.EcmService;
import bio.terra.common.iam.BearerToken;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("pact-test")
@ExtendWith(PactConsumerTestExt.class)
public class TestEcmPact {

  private EcmService ecmService;
  static final String mockProvider = "github"; // TODO: what is this?

  private void initEcmService(MockServer mockServer) {
    EcmServerConfiguration ecmServerConfiguration =
        new EcmServerConfiguration(mockServer.getUrl(), false);

    EcmClient ecmClient = new EcmClient(ecmServerConfiguration);
    ecmService = new EcmService(ecmClient, new BearerToken("accessToken"));
  }

  @Pact(consumer = "cbas", provider = "ecm")
  RequestResponsePact getGithubTokenPact(PactDslWithProvider builder) {
    return builder
        .given(
            State.USER_IS_REGISTERED.toString(),
            Map.of(
                "provider", mockProvider)) // TODO: confirm with Katrina that this state makes sense
        .uponReceiving("a request for an access token")
        .pathFromProviderState(
            "/api/oauth/v1/${provider}/access-token",
            String.format("/api/oauth/v1/%s/access-token", mockProvider))
        .method("GET")
        .willRespondWith()
        .status(200)
        .body("GITHUB_TOKEN")
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getGithubTokenPact", pactVersion = PactSpecVersion.V3)
  void testEcmServiceGetAccessToken(MockServer mockServer) {
    initEcmService(mockServer);
    String githubToken = ecmService.getAccessToken();
    assertEquals("GITHUB_TOKEN", githubToken);
  }
}
