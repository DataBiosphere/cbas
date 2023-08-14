package bio.terra.cbas.dependencies.leonardo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.config.WdsServerConfiguration;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.text.StringSubstitutor;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.junit.jupiter.api.Test;

class TestAppUtils {
  private final String workspaceId = UUID.randomUUID().toString();

  private final LeonardoServerConfiguration leonardoServerConfiguration =
      new LeonardoServerConfiguration("baseuri", List.of("WDS", "CROMWELL"), 0, false);

  private final WdsServerConfiguration wdsServerConfiguration =
      new WdsServerConfiguration("", workspaceId, "", false);
  private final ListAppResponse separatedWdsApp;
  private final ListAppResponse cromwellListAppResponse;

  private final AppUtils au = new AppUtils(leonardoServerConfiguration, wdsServerConfiguration);

  @Test
  void findCromwellUrlInTerraApp() throws Exception {
    List<ListAppResponse> apps = List.of(cromwellListAppResponse);

    AppUtils au = new AppUtils(leonardoServerConfiguration, wdsServerConfiguration);
    assertEquals(anticipatedCromwellUrl("terra-app"), au.findUrlForCromwell(apps));
  }

  @Test
  void findWdsUrlInWdsApp() throws Exception {
    List<ListAppResponse> apps = List.of(separatedWdsApp);

    AppUtils au = new AppUtils(leonardoServerConfiguration, wdsServerConfiguration);
    assertEquals(anticipatedWdsUrl("wds"), au.findUrlForWds(apps));
  }

  @Test
  void findWdsAndCromwellInCombinedAppResponse() throws Exception {
    List<ListAppResponse> apps = List.of(separatedWdsApp, cromwellListAppResponse);

    AppUtils au = new AppUtils(leonardoServerConfiguration, wdsServerConfiguration);
    assertEquals(anticipatedWdsUrl("wds"), au.findUrlForWds(apps));
    assertEquals(anticipatedCromwellUrl("terra-app"), au.findUrlForCromwell(apps));
  }

  private String anticipatedWdsUrl(String appName) {
    return StringSubstitutor.replace(
        "https://lzblahblahblah.servicebus.windows.net/${appName}-${workspaceId}/wds",
        Map.of("workspaceId", workspaceId, "appName", appName));
  }

  private String anticipatedCromwellUrl(String appName) {
    return StringSubstitutor.replace(
        "https://lzblahblahblah.servicebus.windows.net/${appName}-${workspaceId}/cromwell",
        Map.of("workspaceId", workspaceId, "appName", appName));
  }

  public TestAppUtils() throws IOException {
    // Use GSON instead of objectMapper because we want to simulate the JSON that comes back from
    // Leonardo.
    org.broadinstitute.dsde.workbench.client.leonardo.JSON.setGson(new Gson());
    separatedWdsApp =
        ListAppResponse.fromJson(
            StringSubstitutor.replace(
                """
                    {
                        "workspaceId": "${workspaceId}",
                        "cloudContext": {
                            "cloudProvider": "AZURE",
                            "cloudResource": "blah-blah-blah"
                        },
                        "kubernetesRuntimeConfig": {
                            "numNodes": 1,
                            "machineType": "Standard_A2_v2",
                            "autoscalingEnabled": false
                        },
                        "errors": [],
                        "status": "RUNNING",
                        "proxyUrls": {
                            "wds": "https://lzblahblahblah.servicebus.windows.net/wds-${workspaceId}/wds"
                        },
                        "appName": "wds-${workspaceId}",
                        "appType": "WDS",
                        "diskName": null,
                        "auditInfo": {
                            "creator": "me@broadinstitute.org",
                            "createdDate": "2022-10-10T16:01:36.660590Z",
                            "destroyedDate": null,
                            "dateAccessed": "2023-02-09T16:01:36.660590Z"
                        },
                        "accessScope": null,
                        "labels": {}
                    }""",
                Map.of("workspaceId", workspaceId)));

    cromwellListAppResponse =
        ListAppResponse.fromJson(
            StringSubstitutor.replace(
                """
                {
                    "workspaceId": "${workspaceId}",
                    "cloudContext": {
                        "cloudProvider": "AZURE",
                        "cloudResource": "blah-blah-blah"
                    },
                    "kubernetesRuntimeConfig": {
                        "numNodes": 1,
                        "machineType": "Standard_A2_v2",
                        "autoscalingEnabled": false
                    },
                    "errors": [],
                    "status": "RUNNING",
                    "proxyUrls": {
                        "cbas": "https://lzblahblahblah.servicebus.windows.net/terra-app-${workspaceId}/cbas",
                        "cbas-ui": "https://lzblahblahblah.servicebus.windows.net/terra-app-${workspaceId}/",
                        "cromwell": "https://lzblahblahblah.servicebus.windows.net/terra-app-${workspaceId}/cromwell"
                    },
                    "appName": "terra-app-${workspaceId}",
                    "appType": "CROMWELL",
                    "diskName": null,
                    "auditInfo": {
                        "creator": "me@broadinstitute.org",
                        "createdDate": "2023-02-09T16:01:36.660590Z",
                        "destroyedDate": null,
                        "dateAccessed": "2023-02-09T16:01:36.660590Z"
                    },
                    "accessScope": null,
                    "labels": {}
                }""",
                Map.of("workspaceId", workspaceId)));
  }
}
