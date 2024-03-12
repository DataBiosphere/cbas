package bio.terra.cbas.pact;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import bio.terra.cbas.config.SamServerConfiguration;
import bio.terra.cbas.dependencies.sam.SamClient;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.common.iam.BearerToken;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Tag("pact-test")
@ExtendWith(PactConsumerTestExt.class)
class SamPactTest {
  static final String dummyWorkspaceId = "15f36863-30a5-4cab-91f7-52be439f1175";
  private SamService samService;

  @BeforeEach
  void setUp() {
    // Without this setup, the HttpClient throws a "No thread-bound request found" error
    MockHttpServletRequest request = new MockHttpServletRequest();
    // Set the mock request as the current request context
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private void initSamService(MockServer mockServer) {
    SamServerConfiguration samConfig =
        new SamServerConfiguration(mockServer.getUrl(), false, dummyWorkspaceId, true);
    SamClient samClient = new SamClient(samConfig);
    samService = new SamService(samClient, new BearerToken("accessToken"));
  }

  @Pact(consumer = "cbas", provider = "sam")
  RequestResponsePact statusApiPact(PactDslWithProvider builder) {
    return builder
        .given("Sam is ok")
        .uponReceiving("a status request")
        .path("/status")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().booleanValue("ok", true).object("systems"))
        .toPact();
  }

  @Pact(consumer = "cbas", provider = "sam")
  RequestResponsePact writePermissionPact(PactDslWithProvider builder) {
    return builder
        .given("user has write permission", Map.of("dummyResourceId", dummyWorkspaceId))
        .uponReceiving("a request for write permission on workspace")
        .pathFromProviderState(
            "/api/resources/v2/workspace/${dummyResourceId}/action/write",
            String.format("/api/resources/v2/workspace/%s/action/write", dummyWorkspaceId))
        .method("GET")
        .headers("Authorization", "Bearer accessToken")
        .willRespondWith()
        .status(200)
        .body("true")
        .toPact();
  }

  @Pact(consumer = "cbas", provider = "sam")
  RequestResponsePact userStatusPact(PactDslWithProvider builder) {
    var userResponseShape =
        new PactDslJsonBody()
            .stringType("userSubjectId")
            .stringType("userEmail")
            .booleanType("adminEnabled")
            .booleanValue("enabled", true);
    return builder
        .given("user status info request with access token")
        .uponReceiving("a request for the user's status")
        .path("/register/user/v2/self/info")
        .method("GET")
        .headers("Authorization", "Bearer accessToken")
        .willRespondWith()
        .status(200)
        .body(userResponseShape.asBody())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "writePermissionPact", pactVersion = PactSpecVersion.V3)
  void testSamServiceWritePermissionPact(MockServer mockServer) {
    initSamService(mockServer);
    UserStatusInfo mockUserInfo = new UserStatusInfo().userSubjectId("testUser");
    boolean hasWritePermission = samService.hasPermission(SamService.WRITE_ACTION, mockUserInfo);
    assertTrue(hasWritePermission);
  }

  @Test
  @PactTestFor(pactMethod = "statusApiPact", pactVersion = PactSpecVersion.V3)
  void testSamServiceStatusCheck(MockServer mockServer) {
    initSamService(mockServer);
    var system = samService.checkHealth();
    assertTrue(system.isOk());
  }

  @Test
  @PactTestFor(pactMethod = "userStatusPact", pactVersion = PactSpecVersion.V3)
  void testSamServiceUserStatusInfo(MockServer mockServer) {
    initSamService(mockServer);
    UserStatusInfo userInfo =
        assertDoesNotThrow(
            () -> samService.getSamUser(), "get user info request should not throw an error");

    assertNotNull(userInfo);
    assertNotNull(userInfo.getUserEmail());
  }
}
