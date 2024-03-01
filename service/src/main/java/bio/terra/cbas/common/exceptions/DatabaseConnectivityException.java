package bio.terra.cbas.common.exceptions;

import java.util.UUID;

public class DatabaseConnectivityException extends Exception{

  public DatabaseConnectivityException(String message) {
    super(message);
  }

  public static class RunCreationException extends DatabaseConnectivityException {

    public RunCreationException(UUID runId, String recordId) {
      super(
          "Failed to create new Run with ID %s for WDS record %s."
              .formatted(runId, recordId));
    }
  }
}
