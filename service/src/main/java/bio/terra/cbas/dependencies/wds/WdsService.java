package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.AzureAccessTokenException;
import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.dependencies.common.HealthCheck;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.springframework.stereotype.Component;
import javax.ws.rs.ProcessingException;

@Component
public class WdsService implements HealthCheck {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;

  private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WdsService.class);

  public WdsService(WdsClient wdsClient, WdsServerConfiguration wdsServerConfiguration) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  public RecordResponse getRecord(String recordType, String recordId)
      throws ApiException, DependencyNotAvailableException, AzureAccessTokenException {
    return wdsClient
        .recordsApi()
        .getRecord(
            wdsServerConfiguration.instanceId(),
            wdsServerConfiguration.apiV(),
            recordType,
            recordId);
  }

  public RecordResponse updateRecord(RecordRequest request, String type, String id)
      throws ApiException, DependencyNotAvailableException, AzureAccessTokenException {
    return wdsClient
        .recordsApi()
        .updateRecord(
            request, wdsServerConfiguration.instanceId(), wdsServerConfiguration.apiV(), type, id);
  }

  @Override
  public Result checkHealth() {
    try {
      var result = wdsClient.generalWdsInformationApi().versionGet();
      return new Result(true, "WDS version is %s".formatted(result.getBuild().getVersion()));
    } catch (DependencyNotAvailableException | ApiException | AzureAccessTokenException |
    ProcessingException e) {
      logger.error("WDS health check failed", e);
      return new Result(false, e.getMessage());
    }
  }
}
