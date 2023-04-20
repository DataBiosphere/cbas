package bio.terra.cbas.dependencies.common;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.AzureAccessTokenException.NullAzureAccessTokenException;
import bio.terra.cbas.config.AzureCredentialConfig;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/** Strategy for obtaining an access token in an environment with available Azure identity */
@Component
public class CredentialLoader {

  AzureProfile azureProfile = new AzureProfile(AzureEnvironment.AZURE);
  String tokenScope = "https://management.azure.com/.default";

  public enum CredentialType {
    AZURE_TOKEN
  }

  private final LoadingCache<CredentialType, String> cache;

  private final AzureCredentialConfig azureCredentialConfig;

  public CredentialLoader(AzureCredentialConfig azureCredentialConfig) {

    this.azureCredentialConfig = azureCredentialConfig;
    CacheLoader<CredentialType, String> loader =
        new CacheLoader<>() {
          @NotNull
          @Override
          public String load(@NotNull CredentialType key) throws AzureAccessTokenException {
            if (Objects.equals(key, CredentialType.AZURE_TOKEN)) {
              return fetchAzureAccessToken();
            } else {
              throw new AzureAccessTokenException("Unrecognized token key: %s".formatted(key));
            }
          }
        };

    cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(azureCredentialConfig.getTokenCacheTtl())
            .build(loader);
  }

  private TokenRequestContext tokenRequestContext() {
    TokenRequestContext trc = new TokenRequestContext();
    trc.addScopes(tokenScope);
    return trc;
  }

  private DefaultAzureCredentialBuilder defaultCredentialBuilder() {
    return new DefaultAzureCredentialBuilder()
        .authorityHost(azureProfile.getEnvironment().getActiveDirectoryEndpoint());
  }

  String fetchAzureAccessToken() throws AzureAccessTokenException {

    if (azureCredentialConfig.getManualTokenOverride().isPresent()) {
      return azureCredentialConfig.getManualTokenOverride().get();
    } else {
      DefaultAzureCredential credentials = defaultCredentialBuilder().build();

      try {
        AccessToken tokenObject =
            credentials
                .getToken(tokenRequestContext())
                .block(azureCredentialConfig.getTokenAcquisitionTimeout());
        if (tokenObject == null) {
          throw new NullAzureAccessTokenException();
        } else {
          return tokenObject.getToken();
        }
      } catch (RuntimeException e) {
        throw new AzureAccessTokenException("Failed to refresh access token.", e);
      }
    }
  }

  public String getCredential(CredentialType credentialType) throws AzureAccessTokenException {
    try {
      return cache.get(credentialType);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof AzureAccessTokenException aate) {
        throw aate;
      } else {
        cache.invalidate(credentialType);
        throw new AzureAccessTokenException("Unable to fetch token from cache", e);
      }
    }
  }

  public void invalidateCredential(CredentialType credentialType) {
    cache.invalidate(credentialType);
  }
}
