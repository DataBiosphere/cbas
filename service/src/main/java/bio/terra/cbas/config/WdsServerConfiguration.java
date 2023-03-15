package bio.terra.cbas.config;

import bio.terra.cbas.dependencies.common.AzureCredentials;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wds")
public class WdsServerConfiguration {

  private String baseUri;
  private String healthcheckEndpoint;
  private String instanceId;
  private String apiV;
  private String wdsProxyUrlRoot;

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public String getHealthcheckEndpoint() {
    return healthcheckEndpoint;
  }

  public void setHealthcheckEndpoint(String healthcheckEndpoint) {
    this.healthcheckEndpoint = healthcheckEndpoint;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getApiV() {
    return apiV;
  }

  public void setApiV(String apiV) {
    this.apiV = apiV;
  }

  public String getWdsProxyUrlRoot() {
    return wdsProxyUrlRoot;
  }

  private void loadWdsProxyUrlRoot() {
    // if value for `wds.baseUri` is passed, use that as WDS proxy url root otherwise ping Leo to
    // figure out the proxy url. This way
    //  - for local setup we can connect to local WDS setup and
    //  - until we ready to decouple CBAS and WDS in prod, CBAS can keep using the value passed in
    //    config as proxy url. When we want to decouple these apps, remove `wds.baseUri` from
    //    cromwhelm config and CBAS will ping Leo to get proxy url
    if (baseUri != null) this.wdsProxyUrlRoot = baseUri;
    else {
      // call Leo to get apps details
      // call resolveWdsApps to get wds url
    }
  }

  public String healthcheckUri() {
    System.out.printf("WDS baseUri: %s%n", this.baseUri);

    try {
      String token = new AzureCredentials().getAccessToken();
      System.out.printf("AzureCredentials: %s%n", token);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    return "%s/%s".formatted(this.baseUri, this.healthcheckEndpoint);
  }
}
