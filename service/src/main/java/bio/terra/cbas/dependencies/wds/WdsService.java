package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheckable;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.springframework.stereotype.Component;

@Component
public class WdsService implements HealthCheckable {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;

  public WdsService(WdsClient wdsClient, WdsServerConfiguration wdsServerConfiguration) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  public RecordResponse getRecord(String recordType, String recordId)
      throws ApiException, DependencyNotAvailableException, AzureAccessTokenException {
    return wdsClient
        .recordsApi()
        .getRecord(
            wdsServerConfiguration.getInstanceId(),
            wdsServerConfiguration.getApiV(),
            recordType,
            recordId);
  }

  public RecordResponse updateRecord(RecordRequest request, String type, String id)
      throws ApiException, DependencyNotAvailableException, AzureAccessTokenException {
    return wdsClient
        .recordsApi()
        .updateRecord(
            request,
            wdsServerConfiguration.getInstanceId(),
            wdsServerConfiguration.getApiV(),
            type,
            id);
  }

  @Override
  public HealthCheckResult checkHealth() {
    try {
      var result = wdsClient.generalWdsInformationApi().versionGet();
      return new HealthCheckResult(
          true, "WDS version is %s".formatted(result.getBuild().getVersion()));
    } catch (DependencyNotAvailableException | ApiException | AzureAccessTokenException e) {
      return new HealthCheckResult(false, e.getMessage());
    }
  }
}
