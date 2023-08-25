package bio.terra.cbas.dependencies.leonardo;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.config.WdsServerConfiguration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppType;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppUtils {

  private final LeonardoServerConfiguration leonardoServerConfiguration;
  private final WdsServerConfiguration wdsServerConfiguration;

  private static final Logger logger = LoggerFactory.getLogger(AppUtils.class);

  public AppUtils(
      LeonardoServerConfiguration leonardoServerConfiguration,
      WdsServerConfiguration wdsServerConfiguration) {
    this.leonardoServerConfiguration = leonardoServerConfiguration;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  int appComparisonFunction(ListAppResponse a, ListAppResponse b, List<AppType> appTypeList) {
    // First criteria: Prefer apps with the expected app type.
    // NB: Negative because lower index is better
    int appTypeScoreA = -appTypeList.indexOf(a.getAppType());
    int appTypeScoreB = -appTypeList.indexOf(b.getAppType());
    if (appTypeScoreA != appTypeScoreB) {
      return appTypeScoreA - appTypeScoreB;
    }
    // If there is a WDS app type present, do this check; does not apply to cromwell app-types
    if (a.getAppType() == AppType.WDS || b.getAppType() == AppType.WDS) {
      // Second criteria: Prefer apps with the expected app type name
      int nameScoreA =
          Objects.equals(a.getAppName(), "wds-%s".formatted(wdsServerConfiguration.instanceId()))
              ? 1
              : 0;
      int nameScoreB =
          Objects.equals(b.getAppName(), "wds-%s".formatted(wdsServerConfiguration.instanceId()))
              ? 1
              : 0;
      if (nameScoreA != nameScoreB) {
        return nameScoreA - nameScoreB;
      }
    }

    // Third criteria: tie-break on whichever is older
    return OffsetDateTime.parse(a.getAuditInfo().getCreatedDate())
        .compareTo(OffsetDateTime.parse(b.getAuditInfo().getCreatedDate()));
  }

  /**
   * Invokes logic to determine the appropriate app for WDS. If WDS is not running, a URL will not
   * be present, in this case we return empty string Note: This logic is similar to how DataTable
   * finds WDS app in Terra UI
   *
   * <p>(<a
   * href="https://github.com/DataBiosphere/terra-ui/blob/ac13bdf3954788ca7c8fd27b8fd4cfc755f150ff/src/libs/ajax/data-table-providers/WdsDataTableProvider.ts#L94-L147">...</a>)
   */
  ListAppResponse findBestAppForAppType(List<ListAppResponse> apps, AppType appType)
      throws DependencyNotAvailableException {
    // WDS looks for Kubernetes deployment statuses (such as RUNNING or PROVISIONING), expressed by
    // Leo
    // See here for specific enumerations --
    // https://github.com/DataBiosphere/leonardo/blob/develop/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/kubernetesModels.scala
    // look explicitly for a RUNNING app named 'wds-${app.workspaceId}' -- if WDS is healthy and
    // running, there should only be one app RUNNING
    // an app may be in the 'PROVISIONING', 'STOPPED', 'STOPPING', which can still be deemed as an
    // OK state for WDS
    List<AppType> appTypeList;

    if (appType.equals(AppType.WDS)) {
      appTypeList = leonardoServerConfiguration.wdsAppTypeNames();
    } else {
      appTypeList = leonardoServerConfiguration.cromwellRunnerAppTypeNames();
    }

    Set<AppStatus> healthyStates =
        EnumSet.of(
            AppStatus.RUNNING,
            AppStatus.PROVISIONING,
            AppStatus.STARTING,
            AppStatus.STOPPED,
            AppStatus.STOPPING);

    List<ListAppResponse> suitableApps =
        apps.stream()
            .filter(
                app -> {
                  var a = Objects.equals(app.getWorkspaceId(), wdsServerConfiguration.instanceId());
                  if (!a) {
                    logger.info(
                        "Not using app {} for {} because it is in workspace {}, not {}",
                        app.getAppName(),
                        appType,
                        app.getWorkspaceId(),
                        wdsServerConfiguration.instanceId());
                  }
                  var b = appTypeList.contains(app.getAppType());
                  if (!b) {
                    logger.info(
                        "Not using app {} for {} because it is of type {}, not one of {}",
                        app.getAppName(),
                        appType,
                        app.getAppType(),
                        appTypeList);
                  }
                  var c = healthyStates.contains(app.getStatus());
                  if (!c) {
                    logger.info(
                        "Not using app {} for {} because it is in state {}, not one of {}",
                        app.getAppName(),
                        appType,
                        app.getStatus(),
                        healthyStates);
                  }

                  return a && b && c;
                })
            .toList();

    return suitableApps.stream()
        .max((a, b) -> this.appComparisonFunction(a, b, appTypeList))
        .orElseThrow(
            () ->
                new DependencyNotAvailableException(
                    "%s".formatted(appType.toString()),
                    "No suitable, healthy app found for %s (out of %s total apps in this workspace)"
                        .formatted(appType.toString(), apps.size())));
  }

  public String findUrlForWds(List<ListAppResponse> apps) throws DependencyNotAvailableException {
    ListAppResponse foundApp = findBestAppForAppType(apps, AppType.WDS);
    @SuppressWarnings("unchecked")
    Map<String, String> proxyUrls = (foundApp.getProxyUrls());
    if (proxyUrls != null && foundApp.getStatus() == AppStatus.RUNNING) {
      return Optional.ofNullable(proxyUrls.get("wds"))
          .orElseThrow(
              () ->
                  new DependencyNotAvailableException(
                      "WDS",
                      "WDS proxy URL not found in %s app (available proxy URLs: %s)"
                          .formatted(
                              foundApp.getAppName(),
                              proxyUrls.keySet().stream()
                                  .sorted()
                                  .collect(Collectors.joining(", ")))));
    }

    throw new DependencyNotAvailableException(
        "WDS",
        "WDS in %s app not ready (%s)".formatted(foundApp.getAppName(), foundApp.getStatus()));
  }

  public String findUrlForCromwell(List<ListAppResponse> apps)
      throws DependencyNotAvailableException {
    ListAppResponse foundApp = findBestAppForAppType(apps, AppType.CROMWELL);
    Object proxyUrl;

    if (Objects.requireNonNull(foundApp.getProxyUrls()).containsKey("cromwell-writer")) {
      proxyUrl = "cromwell-writer";
    } else {
      proxyUrl = "cromwell";
    }
    // find proper proxy for cromwell app type
    @SuppressWarnings("unchecked")
    Map<String, String> proxyUrls = foundApp.getProxyUrls();
    if (proxyUrls != null && foundApp.getStatus() == AppStatus.RUNNING) {
      return Optional.ofNullable(proxyUrls.get(proxyUrl))
          .orElseThrow(
              () ->
                  new DependencyNotAvailableException(
                      "Cromwell",
                      "Cromwell proxy URL not found in %s app (available proxy URLs: %s)"
                          .formatted(
                              foundApp.getAppName(),
                              proxyUrls.keySet().stream()
                                  .sorted()
                                  .collect(Collectors.joining(", ")))));
    }

    throw new DependencyNotAvailableException(
        "Cromwell",
        "Cromwell in %s app not ready (%s)".formatted(foundApp.getAppName(), foundApp.getStatus()));
  }
}
