package bio.terra.cbas.dependencies.common;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.config.AzureCredentialConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TestCredentialLoader {

  @Test
  void credentialsLoaderFetchesNewTokens() throws Exception {
    TestableCredentialLoader credentialLoader =
        new TestableCredentialLoader(new AzureCredentialConfig(0, 0, null));

    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
  }

  @Test
  void credentialsLoaderCaches() throws Exception {
    var azureCredentialConfig =
        new AzureCredentialConfig(Duration.ZERO, Duration.ofMinutes(10), null);
    TestableCredentialLoader credentialLoader = new TestableCredentialLoader(azureCredentialConfig);

    // Token caching means the token won't be re-fetched on multiple reads:
    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
  }

  @Test
  void credentialsLoaderCacheCanBeFlushed() throws Exception {

    var azureCredentialConfig =
        new AzureCredentialConfig(Duration.ZERO, Duration.ofMinutes(10), null);
    TestableCredentialLoader credentialLoader = new TestableCredentialLoader(azureCredentialConfig);

    // Token caching means the token won't be re-fetched on multiple reads:
    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    credentialLoader.invalidateCredential(CredentialLoader.CredentialType.AZURE_TOKEN);
    assertEquals(
        "TOKEN_2", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    assertEquals(
        "TOKEN_2", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    credentialLoader.invalidateCredential(CredentialLoader.CredentialType.AZURE_TOKEN);
    assertEquals(
        "TOKEN_3", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    assertEquals(
        "TOKEN_3", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
  }

  @Test
  void credentialsLoaderCacheTimesOutPerConfiguration() throws Exception {

    var azureCredentialConfig =
        new AzureCredentialConfig(Duration.ZERO, Duration.ofMillis(100), null);
    TestableCredentialLoader credentialLoader = new TestableCredentialLoader(azureCredentialConfig);

    // Token caching means the token won't be re-fetched on multiple reads:
    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));
    assertEquals(
        "TOKEN_1", credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN));

    await()
        .atMost(200, MILLISECONDS)
        .until(
            () -> credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN),
            equalTo("TOKEN_2"));
    await()
        .atMost(200, MILLISECONDS)
        .until(
            () -> credentialLoader.getCredential(CredentialLoader.CredentialType.AZURE_TOKEN),
            equalTo("TOKEN_3"));
  }

  private static class TestableCredentialLoader extends CredentialLoader {

    private int tokenCounter = 1;

    public TestableCredentialLoader(AzureCredentialConfig azureCredentialConfig) {
      super(azureCredentialConfig);
    }

    @Override
    String fetchAzureAccessToken() {
      String tokenValuePrefix = "TOKEN_";
      return tokenValuePrefix + tokenCounter++;
    }
  }
}
