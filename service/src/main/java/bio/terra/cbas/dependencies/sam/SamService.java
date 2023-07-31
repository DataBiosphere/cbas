package bio.terra.cbas.dependencies.sam;

import bio.terra.cbas.dependencies.common.HealthCheck;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.springframework.stereotype.Component;

@Component
public class SamService implements HealthCheck {

  private final SamClient samClient;

  public SamService(SamClient samClient) {
    this.samClient = samClient;
  }

  private StatusApi getStatusApi() {
    return new StatusApi(samClient.getApiClient());
  }

  @Override
  public Result checkHealth() {
    try {
      SystemStatus result = getStatusApi().getSystemStatus();
      return new Result(result.getOk(), result.toString());
    } catch (ApiException e) {
      return new Result(false, e.getMessage());
    }
  }
}
