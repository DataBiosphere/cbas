package bio.terra.cbas.common.exceptions;

import java.util.UUID;

public class DatabaseConnectivityException extends Exception {

  public DatabaseConnectivityException(String message) {
    super(message);
  }

  public static class RunSetCreationException extends DatabaseConnectivityException {

    public RunSetCreationException(String runSetName) {
      super("Failed to create new RunSet for '%s'.".formatted(runSetName));
    }
  }

  public static class RunCreationException extends DatabaseConnectivityException {

    public RunCreationException(UUID runSetId, UUID runId, String recordId) {
      super(
          "Failed to create new Run with ID %s for WDS record %s in RunSet %s."
              .formatted(runId, recordId, runSetId));
    }
  }
}
