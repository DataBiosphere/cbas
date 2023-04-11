package bio.terra.cbas.dependencies.leonardo;

import bio.terra.cbas.config.WdsServerConfiguration;
import cromwell.client.model.*;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.api.AppsV2Api;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.springframework.stereotype.Component;

@Component
public class LeonardoService {

  private final AppsV2Api appsV2Api;

  private final WdsServerConfiguration wdsServerConfiguration;

  public LeonardoService(
      LeonardoClient leonardoClient, WdsServerConfiguration wdsServerConfiguration) {
    this.appsV2Api = new AppsV2Api(leonardoClient.getApiClient());
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  public List<ListAppResponse> getApps() throws ApiException {
    return appsV2Api.listAppsV2(wdsServerConfiguration.getInstanceId(), null, null, null);
  }
}
