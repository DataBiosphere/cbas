package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.config.WdsServerConfiguration;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.EntityResponse;
import org.springframework.stereotype.Component;

@Component
public class WdsService {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;

  public WdsService(WdsClient wdsClient, WdsServerConfiguration wdsServerConfiguration) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  public EntityResponse getEntity(String entityType, String entityId) throws ApiException {
    return wdsClient
        .entitiesApi()
        .getEntity(
            wdsServerConfiguration.instanceId(),
            wdsServerConfiguration.apiV(),
            entityType,
            entityId);
  }
}
