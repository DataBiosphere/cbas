package bio.terra.cbas.common.exceptions;

public class DockstoreDescriptorException extends Exception {

  public DockstoreDescriptorException(String message) {
    super(message);
  }

  public static class DockstoreDescriptorNotFoundException extends DockstoreDescriptorException {

    public DockstoreDescriptorNotFoundException(String dockstoreWorkflowPath) {
      super("Descriptor not found for Dockstore workflow at path '" + dockstoreWorkflowPath + "'");
    }
  }
}
