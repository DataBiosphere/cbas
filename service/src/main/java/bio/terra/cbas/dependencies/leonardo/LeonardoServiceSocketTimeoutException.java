package bio.terra.cbas.dependencies.leonardo;

import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;

public class LeonardoServiceSocketTimeoutException extends LeonardoServiceException {

  public LeonardoServiceSocketTimeoutException(ApiException e) {
    super("Leonardo API call timed out and returned SocketTimeoutException", e);
  }
}
