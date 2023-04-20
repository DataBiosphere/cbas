package bio.terra.cbas.dependencies.common;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.dependencies.leonardo.AppUtils;
import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import java.time.Duration;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestDependencyUrlLoader {

  LeonardoServerConfiguration leonardoServerConfiguration;
  LeonardoService leonardoService;
  AppUtils appUtils;

  @BeforeEach
  void setup() {
    leonardoService = mock(LeonardoService.class);
    appUtils = mock(AppUtils.class);
    leonardoServerConfiguration = mock(LeonardoServerConfiguration.class);
  }

  @Test
  void fetchesUrlsAppropriately() throws Exception {

    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a token for testing that the wiring is happening properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    String discoveredWdsUrl = "https://wds.com/prefix/wds";

    when(leonardoService.getApps()).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl);
    when(leonardoServerConfiguration.getDependencyUrlCacheTtl()).thenReturn(Duration.ofMinutes(10));

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    assertEquals(
        discoveredWdsUrl,
        dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL));
  }

  @Test
  void cachesDependencyUrls() throws Exception {

    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a token for testing that the wiring is happening properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    String discoveredWdsUrl = "https://wds.com/prefix/wds";

    when(leonardoService.getApps()).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl);
    when(leonardoServerConfiguration.getDependencyUrlCacheTtl()).thenReturn(Duration.ofMinutes(10));

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    assertEquals(
        discoveredWdsUrl,
        dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL));

    // Even if the app utils would return something new, the dependency loader returns the cached
    // value:
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn("https://some-other-url");
    assertEquals(
        discoveredWdsUrl,
        dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL));
  }

  @Test
  void dependencyUrlCacheExpires() throws Exception {
    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a token for testing that the wiring is happening properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    String discoveredWdsUrl1 = "https://wds.com/prefix/wds";
    String discoveredWdsUrl2 = "https://new-wds.com/prefix/wds";

    when(leonardoService.getApps()).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl1);
    when(leonardoServerConfiguration.getDependencyUrlCacheTtl()).thenReturn(Duration.ofMillis(100));

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    assertEquals(
        discoveredWdsUrl1,
        dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL));

    // Even if the app utils would return something new, the dependency loader returns the cached
    // value:
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl2);
    assertEquals(
        discoveredWdsUrl1,
        dependencyUrlLoader.loadDependencyUrl(DependencyUrlLoader.DependencyUrlType.WDS_URL));

    // But eventually, the cache expires and we DO get the new value:
    await()
        .atMost(200, MILLISECONDS)
        .until(
            () ->
                dependencyUrlLoader.loadDependencyUrl(
                    DependencyUrlLoader.DependencyUrlType.WDS_URL),
            equalTo(discoveredWdsUrl2));
  }

  @Test
  void wrapsExceptionsFromLeonardo() throws Exception {
    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a token for testing that the wiring is happening properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    String discoveredWdsUrl = "https://wds.com/prefix/wds";

    when(leonardoService.getApps()).thenThrow(new ApiException("Bad Leonardo!"));
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl);
    when(leonardoServerConfiguration.getDependencyUrlCacheTtl()).thenReturn(Duration.ofMinutes(10));

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    DependencyNotAvailableException thrown =
        assertThrows(
            DependencyNotAvailableException.class,
            () ->
                dependencyUrlLoader.loadDependencyUrl(
                    DependencyUrlLoader.DependencyUrlType.WDS_URL));

    assertEquals(
        "Dependency not available: WDS. Failed to poll Leonardo for URL", thrown.getMessage());
    assertTrue(thrown.getCause().getMessage().contains("Bad Leonardo!"));
  }

  @Test
  void forwardsExceptionsFromAppUtils() throws Exception {
    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a token for testing that the wiring is happening properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    DependencyNotAvailableException dnae =
        new DependencyNotAvailableException("WDS", "App util lookup failed");

    when(leonardoService.getApps()).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenThrow(dnae);
    when(leonardoServerConfiguration.getDependencyUrlCacheTtl()).thenReturn(Duration.ofMinutes(10));

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    DependencyNotAvailableException thrown =
        assertThrows(
            DependencyNotAvailableException.class,
            () ->
                dependencyUrlLoader.loadDependencyUrl(
                    DependencyUrlLoader.DependencyUrlType.WDS_URL));

    assertEquals(dnae, thrown);
  }
}
