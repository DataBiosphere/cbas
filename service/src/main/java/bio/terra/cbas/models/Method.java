package bio.terra.cbas.models;

import java.util.UUID;

public record Method(
    UUID id, String methodUrl, String inputDefinition, String outputDefinition, String recordType) {

  // Corresponding table column names in database
  public static final String ID = "id";
  public static final String METHOD_URL = "method_url";
  public static final String INPUT_DEFINITION = "input_definition";
  public static final String OUTPUT_DEFINITION = "output_definition";
  public static final String RECORD_TYPE = "record_type";
}
