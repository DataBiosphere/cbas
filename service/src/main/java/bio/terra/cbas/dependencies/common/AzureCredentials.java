package bio.terra.cbas.dependencies.common;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.AzureAccessTokenException.NullAzureAccessTokenException;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.time.Duration;

/** Strategy for obtaining an access token in an environment with available Azure identity */
public final class AzureCredentials {
  final Duration tokenAcquisitionTimeout = Duration.ofSeconds(5);

  AzureProfile azureProfile = new AzureProfile(AzureEnvironment.AZURE);
  String tokenScope = "https://management.azure.com/.default";

  private TokenRequestContext tokenRequestContext() {
    TokenRequestContext trc = new TokenRequestContext();
    trc.addScopes(tokenScope);
    return trc;
  }

  private DefaultAzureCredentialBuilder defaultCredentialBuilder() {
    return new DefaultAzureCredentialBuilder()
        .authorityHost(azureProfile.getEnvironment().getActiveDirectoryEndpoint());
  }

  public String getAccessToken() throws AzureAccessTokenException {
    DefaultAzureCredential credentials = defaultCredentialBuilder().build();

    try {
      AccessToken tokenObject =
          credentials.getToken(tokenRequestContext()).block(tokenAcquisitionTimeout);
      if (tokenObject == null) {
        throw new NullAzureAccessTokenException();
      } else {
        return tokenObject.getToken();
      }
    } catch (RuntimeException e) {
      throw new AzureAccessTokenException(
          "Failed to refresh access token: %s".formatted(e.getMessage()));
    }
  }
}
