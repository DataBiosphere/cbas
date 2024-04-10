package bio.terra.cbas.pact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
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
// import bio.terra.externalcreds.pact.ProviderStates;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("pact-test")
@ExtendWith(PactConsumerTestExt.class)
public class EcmPactTest {

  private EcmService ecmService;

  private void initEcmService(MockServer mockServer) {
    EcmServerConfiguration ecmServerConfiguration =
        new EcmServerConfiguration(mockServer.getUrl(), false);

    EcmClient ecmClient = new EcmClient(ecmServerConfiguration);
    ecmService = new EcmService(ecmClient, new BearerToken("accessToken"));
  }

  @Pact(consumer = "cbas", provider = "ecm")
  RequestResponsePact getStatus(PactDslWithProvider builder) {
    return builder
        // .given(ProviderStates.ECM_IS_OK)
        .given("ECM is ok")
        .uponReceiving("a status request")
        .path("/status")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().booleanValue("ok", true).object("systems"))
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getStatus", pactVersion = PactSpecVersion.V3)
  void testEcmServiceGetStatus(MockServer mockServer) {
    initEcmService(mockServer);
    var system = ecmService.checkHealth();
    assertTrue(system.isOk());
  }

  @Pact(consumer = "cbas", provider = "ecm")
  RequestResponsePact getGithubAccessToken(PactDslWithProvider builder) {
    return builder
        // .given(ProviderStates.USER_IS_REGISTERED)
        .given("test_user@test.com is registered with ECM")
        .uponReceiving("a github token request")
        .path("/api/oauth/v1/github/access-token")
        .headers("Authorization", "Bearer accessToken")
        .method("GET")
        .willRespondWith()
        .status(200)
        .bodyMatchingContentType("text/plain", "GITHUB_TOKEN")
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getGithubAccessToken", pactVersion = PactSpecVersion.V3)
  void testEcmServiceGetAccessToken(MockServer mockServer) {
    initEcmService(mockServer);
    var token = ecmService.getAccessToken();
    assertEquals("GITHUB_TOKEN", token);
  }
}
