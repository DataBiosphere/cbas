package bio.terra.cbas.dependencies.leonardo;

import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;

public class LeonardoServiceApiException extends LeonardoServiceException {

  public LeonardoServiceApiException(ApiException exception) {
    super("Leonardo returned an unsuccessful status code", exception);
  }
}
