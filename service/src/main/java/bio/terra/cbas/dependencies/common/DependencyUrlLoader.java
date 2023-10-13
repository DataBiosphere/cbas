package bio.terra.cbas.dependencies.common;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.dependencies.leonardo.AppUtils;
import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import bio.terra.cbas.dependencies.leonardo.LeonardoServiceException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DependencyUrlLoader {

  public enum DependencyUrlType {
    WDS_URL,
    CROMWELL_URL
  }

  private record DependencyCacheKey(DependencyUrlType urlType, String userToken) {}

  private final LoadingCache<DependencyCacheKey, String> cache;

  private final LeonardoService leonardoService;
  private final AppUtils appUtils;

  public DependencyUrlLoader(
      LeonardoService leonardoService,
      AppUtils appUtils,
      LeonardoServerConfiguration leonardoServerConfiguration) {
    this.leonardoService = leonardoService;
    this.appUtils = appUtils;

    CacheLoader<DependencyCacheKey, String> loader =
        new CacheLoader<>() {
          @NotNull
          @Override
          public String load(DependencyCacheKey key) throws DependencyNotAvailableException {
            if (key.urlType == DependencyUrlType.WDS_URL) {
              return fetchWdsUrl();
            } else if (key.urlType == DependencyUrlType.CROMWELL_URL) {
              return fetchCromwellUrl();
            }
            throw new DependencyNotAvailableException(
                key.urlType.toString(), "Unknown dependency URL type");
          }
        };

    cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(leonardoServerConfiguration.dependencyUrlCacheTtl())
            .build(loader);
  }

  private String fetchWdsUrl() throws DependencyNotAvailableException {
    try {
      List<ListAppResponse> allApps = leonardoService.getApps(false);
      return appUtils.findUrlForWds(allApps);
    } catch (LeonardoServiceException e) {
      throw new DependencyNotAvailableException("WDS", "Failed to poll Leonardo for URL", e);
    }
  }

  private String fetchCromwellUrl() throws DependencyNotAvailableException {
    try {
      List<ListAppResponse> allApps = leonardoService.getApps(true);
      return appUtils.findUrlForCromwell(allApps);
    } catch (LeonardoServiceException e) {
      throw new DependencyNotAvailableException("CROMWELL", "Failed to poll Leonardo for URL", e);
    }
  }

  public String loadDependencyUrl(DependencyUrlType urlType, String accessToken)
      throws DependencyNotAvailableException {
    try {
      return cache.get(new DependencyCacheKey(urlType, accessToken));
    } catch (ExecutionException | UncheckedExecutionException e) {
      if (e.getCause() instanceof DependencyNotAvailableException dnae) {
        throw dnae;
      } else {
        throw new DependencyNotAvailableException(
            urlType.toString(), "Failed to lookup URL in cache", e);
      }
    }
  }

  public void flushBadDependencyFromCache(DependencyUrlType urlType) {
    cache.invalidate(urlType);
  }
}
