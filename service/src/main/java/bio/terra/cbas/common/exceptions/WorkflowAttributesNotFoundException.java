package bio.terra.cbas.common.exceptions;

public class WorkflowAttributesNotFoundException extends Exception {

  public WorkflowAttributesNotFoundException(String attribute, String recordId, String inputName) {
    super("Attribute %s not found in WDS record %s (to populate workflow input %s).".formatted(attribute, recordId, inputName));
  }
}
