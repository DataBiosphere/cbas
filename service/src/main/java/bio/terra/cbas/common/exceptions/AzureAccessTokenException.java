package bio.terra.cbas.common.exceptions;

public class AzureAccessTokenException extends Exception {

  public AzureAccessTokenException(String message) {
    super(message);
  }

  public static class NullAzureAccessTokenException extends AzureAccessTokenException {
    public NullAzureAccessTokenException() {
      super("Null token value received when attempting to obtain Azure access token.");
    }
  }
}
