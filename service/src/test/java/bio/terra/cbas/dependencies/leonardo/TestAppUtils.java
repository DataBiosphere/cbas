package bio.terra.cbas.dependencies.leonardo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.LeonardoServerConfiguration;
import bio.terra.cbas.config.WdsServerConfiguration;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.text.StringSubstitutor;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.junit.jupiter.api.Test;

class TestAppUtils {
  private final String workspaceId = UUID.randomUUID().toString();

  private final LeonardoServerConfiguration leonardoServerConfiguration =
      new LeonardoServerConfiguration()
          .baseUri("baseuri")
          .wdsAppTypeNames(List.of("WDS", "CROMWELL"));

  private final WdsServerConfiguration wdsServerConfiguration =
      new WdsServerConfiguration().instanceId(workspaceId);

  private final ListAppResponse combinedWdsInCromwellApp;
  private final ListAppResponse otherNamedCromwellApp;
  private final ListAppResponse galaxyApp;
  private final ListAppResponse otherNamedCromwellAppOlder;
  private final ListAppResponse otherNamedCromwellAppProvisioning;
  private final ListAppResponse separatedWdsApp;
  private final ListAppResponse separatedWorkflowsApp;

  private final AppUtils au = new AppUtils(leonardoServerConfiguration, wdsServerConfiguration);

  @Test
  void findWdsUrlInCombinedApp() throws Exception {
    List<ListAppResponse> apps = List.of(combinedWdsInCromwellApp);

    AppUtils au = new AppUtils(leonardoServerConfiguration, wdsServerConfiguration);
    assertEquals(anticipatedWdsUrl("wds"), au.findUrlForWds(apps));
  }

  @Test
  void findWdsUrlInOtherNamedApp() throws Exception {
    List<ListAppResponse> apps = List.of(otherNamedCromwellApp);

    AppUtils au = new AppUtils(leonardoServerConfiguration, wdsServerConfiguration);
    assertEquals(anticipatedWdsUrl("app1"), au.findUrlForWds(apps));
  }

  @Test
  void preferSpecificallyNamedApp() throws Exception {
    List<ListAppResponse> apps =
        new java.util.ArrayList<>(List.of(combinedWdsInCromwellApp, otherNamedCromwellApp));
    // Shuffle to make sure the initial ordering isn't relevant:
    Collections.shuffle(apps);
    assertEquals(anticipatedWdsUrl("wds"), au.findUrlForWds(apps));
  }

  @Test
  void notChooseGalaxyApp() throws Exception {
    List<ListAppResponse> apps =
        new java.util.ArrayList<>(List.of(otherNamedCromwellApp, galaxyApp));
    // Shuffle to make sure the initial ordering isn't relevant:
    Collections.shuffle(apps);
    assertEquals(anticipatedWdsUrl("app1"), au.findUrlForWds(apps));
  }

  @Test
  void preferNewerCreatedApp() throws Exception {
    List<ListAppResponse> apps =
        new java.util.ArrayList<>(List.of(otherNamedCromwellApp, otherNamedCromwellAppOlder));
    // Shuffle to make sure the initial ordering isn't relevant:
    Collections.shuffle(apps);
    assertEquals(anticipatedWdsUrl("app1"), au.findUrlForWds(apps));
  }

  @Test
  void preferWdsAppOverCromwell() throws Exception {
    List<ListAppResponse> apps =
        new java.util.ArrayList<>(List.of(separatedWdsApp, separatedWorkflowsApp));
    // Shuffle to make sure the initial ordering isn't relevant:
    Collections.shuffle(apps);
    assertEquals(anticipatedWdsUrl("wds"), au.findUrlForWds(apps));
  }

  @Test
  void throwIfBestAppNotReady() throws Exception {
    List<ListAppResponse> apps =
        new java.util.ArrayList<>(
            List.of(otherNamedCromwellAppOlder, otherNamedCromwellAppProvisioning));
    // Shuffle to make sure the initial ordering isn't relevant:
    Collections.shuffle(apps);

    assertThrows(
        DependencyNotAvailableException.class,
        () -> {
          au.findUrlForWds(apps);
        });
  }

  @Test
  void throwIfBestAppHasNoWDS() throws Exception {
    List<ListAppResponse> apps = new java.util.ArrayList<>(List.of(separatedWorkflowsApp));
    // Shuffle to make sure the initial ordering isn't relevant:
    Collections.shuffle(apps);

    assertThrows(
        DependencyNotAvailableException.class,
        () -> {
          au.findUrlForWds(apps);
        });
  }

  private String anticipatedWdsUrl(String appName) {
    return StringSubstitutor.replace(
        "https://lzblahblahblah.servicebus.windows.net/${appName}-${workspaceId}/wds",
        Map.of("workspaceId", workspaceId, "appName", appName));
  }

  public TestAppUtils() throws IOException {
    org.broadinstitute.dsde.workbench.client.leonardo.JSON.setGson(new Gson());

    combinedWdsInCromwellApp =
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
                "cbas": "https://lzblahblahblah.servicebus.windows.net/wds-${workspaceId}/cbas",
                "cbas-ui": "https://lzblahblahblah.servicebus.windows.net/wds-${workspaceId}/",
                "cromwell": "https://lzblahblahblah.servicebus.windows.net/wds-${workspaceId}/cromwell",
                "wds": "https://lzblahblahblah.servicebus.windows.net/wds-${workspaceId}/wds"
            },
            "appName": "wds-${workspaceId}",
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

    otherNamedCromwellApp =
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
                "cbas": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/cbas",
                "cbas-ui": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/",
                "cromwell": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/cromwell",
                "wds": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/wds"
            },
            "appName": "app1-${workspaceId}",
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

    galaxyApp =
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
                "blah": "blah blah"
            },
            "appName": "galaxy-${workspaceId}",
            "appType": "GALAXY",
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

    otherNamedCromwellAppProvisioning =
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
            "status": "PROVISIONING",
            "proxyUrls": {
                "cbas": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/cbas",
                "cbas-ui": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/",
                "cromwell": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/cromwell",
                "wds": "https://lzblahblahblah.servicebus.windows.net/app1-${workspaceId}/wds"
            },
            "appName": "app1-${workspaceId}",
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

    otherNamedCromwellAppOlder =
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
                "cbas": "https://lzblahblahblah.servicebus.windows.net/app2-${workspaceId}/cbas",
                "cbas-ui": "https://lzblahblahblah.servicebus.windows.net/app2-${workspaceId}/",
                "cromwell": "https://lzblahblahblah.servicebus.windows.net/app2-${workspaceId}/cromwell",
                "wds": "https://lzblahblahblah.servicebus.windows.net/app2-${workspaceId}/wds"
            },
            "appName": "app2-${workspaceId}",
            "appType": "CROMWELL",
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

    separatedWorkflowsApp =
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
                "cbas": "https://lzblahblahblah.servicebus.windows.net/workflows-${workspaceId}/cbas",
                "cbas-ui": "https://lzblahblahblah.servicebus.windows.net/workflows-${workspaceId}/",
                "cromwell": "https://lzblahblahblah.servicebus.windows.net/workflows-${workspaceId}/cromwell"
            },
            "appName": "workflows-${workspaceId}",
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
