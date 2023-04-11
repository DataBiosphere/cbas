package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordRequest;
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

  public RecordResponse getRecord(String recordType, String recordId)
      throws ApiException, DependencyNotAvailableException {
    return wdsClient
        .recordsApi()
        .getRecord(
            wdsServerConfiguration.getInstanceId(),
            wdsServerConfiguration.getApiV(),
            recordType,
            recordId);
  }

  public RecordResponse updateRecord(RecordRequest request, String type, String id)
      throws ApiException, DependencyNotAvailableException {
    return wdsClient
        .recordsApi()
        .updateRecord(
            request,
            wdsServerConfiguration.getInstanceId(),
            wdsServerConfiguration.getApiV(),
            type,
            id);
  }
}
