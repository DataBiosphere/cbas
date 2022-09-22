package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.config.WdsServerConfiguration;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.springframework.stereotype.Component;

@Component
public class WdsService {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;

  public WdsService(WdsClient wdsClient, WdsServerConfiguration wdsServerConfiguration) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  public RecordResponse getRecord(String entityType, String entityId) throws ApiException {
    return wdsClient
        .recordsApi()
        .getRecord(
            wdsServerConfiguration.instanceId(),
            wdsServerConfiguration.apiV(),
            entityType,
            entityId);
  }
}
