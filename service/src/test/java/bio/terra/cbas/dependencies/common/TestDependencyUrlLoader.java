package bio.terra.cbas.dependencies.common;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.dependencies.leonardo.AppUtils;
import bio.terra.cbas.dependencies.leonardo.LeonardoService;
import bio.terra.cbas.dependencies.leonardo.LeonardoServiceApiException;
import java.time.Duration;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestDependencyUrlLoader {

  @Mock private LeonardoService leonardoService;
  @Mock private AppUtils appUtils;

  @Test
  void fetchesUrlsAppropriately() throws Exception {
    String accessToken = "some-access-token";

    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a placeholder for testing that the wiring is happening
    // properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    String discoveredWdsUrl = "https://wds.com/prefix/wds";
    String discoveredCromwellUrl = "https://cromwell.com/prefix/cromwell";

    when(leonardoService.getApps(any())).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl);
    when(appUtils.findUrlForCromwell(dummyListAppResponse)).thenReturn(discoveredCromwellUrl);
    var leonardoServerConfiguration =
        new LeonardoServerConfiguration("", List.of(), List.of(), Duration.ofMinutes(10), false);

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    assertEquals(
        discoveredWdsUrl,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken));

    assertEquals(
        discoveredCromwellUrl,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.CROMWELL_URL, accessToken));
  }

  @Test
  void cachesDependencyUrls() throws Exception {
    String accessToken = "some-access-token";

    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a placeholder for testing that the wiring is happening
    // properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    String discoveredWdsUrl = "https://wds.com/prefix/wds";

    when(leonardoService.getApps(any())).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl);
    var leonardoServerConfiguration =
        new LeonardoServerConfiguration("", List.of(), List.of(), Duration.ofMinutes(10), false);

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    assertEquals(
        discoveredWdsUrl,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken));

    // Load the same URL again:
    assertEquals(
        discoveredWdsUrl,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken));

    // Assert that the backing call to Leonardo was only made once:
    verify(leonardoService, times(1)).getApps(any());
  }

  @Test
  void doesntCacheBetweenTokens() throws Exception {
    String accessToken1 = "some-access-token";
    String accessToken2 = "some-other-access-token";

    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a placeholder for testing that the wiring is happening
    // properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse().appName("wds1"));
    List<ListAppResponse> dummyListAppResponse2 = List.of(new ListAppResponse().appName("wds2"));
    String discoveredWdsUrl1 = "https://wds.com/prefix/wds1";
    String discoveredWdsUrl2 = "https://wds.com/prefix/wds2";

    when(leonardoService.getApps(any()))
        .thenReturn(dummyListAppResponse)
        .thenReturn(dummyListAppResponse2);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl1);
    when(appUtils.findUrlForWds(dummyListAppResponse2)).thenReturn(discoveredWdsUrl2);

    var leonardoServerConfiguration =
        new LeonardoServerConfiguration("", List.of(), List.of(), Duration.ofMinutes(10), false);

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    assertEquals(
        discoveredWdsUrl1,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken1));

    // Load the same dependency type again, but with a different token:
    assertEquals(
        discoveredWdsUrl2,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken2));

    // Assert that the backing call to Leonardo was only made twice:
    verify(leonardoService, times(2)).getApps(any());
  }

  @Test
  void dependencyUrlCacheExpires() throws Exception {
    String accessToken = "some-access-token";

    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a placeholder for testing that the wiring is happening
    // properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    String discoveredWdsUrl1 = "https://wds.com/prefix/wds";
    String discoveredWdsUrl2 = "https://new-wds.com/prefix/wds";

    when(leonardoService.getApps(any())).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl1);

    var leonardoServerConfiguration =
        new LeonardoServerConfiguration("", List.of(), List.of(), Duration.ofMillis(100), false);

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    assertEquals(
        discoveredWdsUrl1,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken));

    // Even if the app utils would return something new, the dependency loader returns the cached
    // value:
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenReturn(discoveredWdsUrl2);

    assertEquals(
        discoveredWdsUrl1,
        dependencyUrlLoader.loadDependencyUrl(
            DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken));

    // But eventually, the cache expires and we DO get the new value:
    await()
        .atMost(200, MILLISECONDS)
        .until(
            () ->
                dependencyUrlLoader.loadDependencyUrl(
                    DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken),
            equalTo(discoveredWdsUrl2));
  }

  @Test
  void wrapsExceptionsFromLeonardo() throws Exception {
    String accessToken = "some-access-token";
    when(leonardoService.getApps(any()))
        .thenThrow(new LeonardoServiceApiException(new ApiException(400, "Bad Leonardo!")));
    var leonardoServerConfiguration =
        new LeonardoServerConfiguration("", List.of(), List.of(), Duration.ofMinutes(10), false);

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    DependencyNotAvailableException thrown =
        assertThrows(
            DependencyNotAvailableException.class,
            () ->
                dependencyUrlLoader.loadDependencyUrl(
                    DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken));

    assertEquals(
        "Dependency not available: WDS. Failed to poll Leonardo for URL", thrown.getMessage());
    assertTrue(
        thrown.getCause().getMessage().contains("Leonardo returned an unsuccessful status code"));
    assertTrue(thrown.getCause().getCause().getMessage().contains("Bad Leonardo!"));
  }

  @Test
  void forwardsExceptionsFromAppUtils() throws Exception {
    String accessToken = "some-access-token";

    // Doesn't actually matter what this is, we can force the app utils mock to create whatever
    // url we want. This is really just a placeholder for testing that the wiring is happening
    // properly
    // between the mocks.
    List<ListAppResponse> dummyListAppResponse = List.of(new ListAppResponse());
    DependencyNotAvailableException dnae =
        new DependencyNotAvailableException("WDS", "App util lookup failed");

    when(leonardoService.getApps(any())).thenReturn(dummyListAppResponse);
    when(appUtils.findUrlForWds(dummyListAppResponse)).thenThrow(dnae);
    var leonardoServerConfiguration =
        new LeonardoServerConfiguration("", List.of(), List.of(), Duration.ofMinutes(10), false);

    DependencyUrlLoader dependencyUrlLoader =
        new DependencyUrlLoader(leonardoService, appUtils, leonardoServerConfiguration);

    DependencyNotAvailableException thrown =
        assertThrows(
            DependencyNotAvailableException.class,
            () ->
                dependencyUrlLoader.loadDependencyUrl(
                    DependencyUrlLoader.DependencyUrlType.WDS_URL, accessToken));

    assertEquals(dnae, thrown);
  }
}
