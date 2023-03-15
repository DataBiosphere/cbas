package bio.terra.cbas.common;

import java.util.Date;
import java.util.List;

public final class AppUtils {

  private AppUtils() {}

  /**
   * Invokes logic to determine the appropriate app for WDS.
   * If WDS is not running, a URL will not be present, in this case we return empty string
   * Note: This logic is similar to how DataTable finds WDS app in Terra UI
   * (https://github.com/DataBiosphere/terra-ui/blob/ac13bdf3954788ca7c8fd27b8fd4cfc755f150ff/src/libs/ajax/data-table-providers/WdsDataTableProvider.ts#L94-L147)
   */
  // TODO: Once we have Leonardo client make sure that data type for apps is correct
  private static Object resolveWdsApp(List<Object> apps) {
    // WDS looks for Kubernetes deployment statuses (such as RUNNING or PROVISIONING), expressed by Leo
    // See here for specific enumerations -- https://github.com/DataBiosphere/leonardo/blob/develop/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/kubernetesModels.scala
    // look explicitly for a RUNNING app named 'wds-${app.workspaceId}' -- if WDS is healthy and running, there should only be one app RUNNING
    // an app may be in the 'PROVISIONING', 'STOPPED', 'STOPPING', which can still be deemed as an OK state for WDS
    List<String> healthyStates = List.of("RUNNING", "PROVISIONING", "STOPPED", "STOPPING");
    List<String> nonRunningHealthyStates = List.of("RUNNING", "PROVISIONING", "STOPPED", "STOPPING");
    List<Object> namedApp = apps.stream().filter(app -> app.appType == "CROMWELL" && app.appName == "wds-${app.workspaceId}" && healthyStates.contains(app.status));
    if (namedApp.size() == 1) {
      return namedApp.get(0);
    }

    //Failed to find an app with the proper name, look for a RUNNING CROMWELL app
    List<Object> runningWdsApps = apps.stream().filter(app -> app.appType == "CROMWELL" && app.status == "RUNNING");
    if (runningWdsApps.size() > 0) {
      // Evaluate the earliest-created WDS app
      runningWdsApps.sort((a, b) -> new Date(a.auditInfo.createdDate) - new Date(b.auditInfo.createdDate));
      return runningWdsApps.get(0);
    }

    // If we reach this logic, we have more than one Leo app with the associated workspace Id...
    List<Object> allWdsApps = apps.stream().filter(app -> app.appType == "CROMWELL" && nonRunningHealthyStates.contains(app.status));
    if (allWdsApps.size() > 0) {
      // Evaluate the earliest-created WDS app
      allWdsApps.sort((a, b) -> new Date(a.auditInfo.createdDate) - new Date(b.auditInfo.createdDate));
      return allWdsApps.get(0);
    }

    return null;
  }

  /**
   * Extract WDS proxy URL from Leo response. Exported for testing
   */
  public static String resolveWdsUrl(List<Object> apps) {
    Object foundApp = resolveWdsApp(apps);
    if (foundApp != null & foundApp.status == "RUNNING") {
      return foundApp.proxyUrls.wds;
    }
    return "";
  }
}
