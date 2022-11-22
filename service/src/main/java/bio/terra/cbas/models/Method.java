package bio.terra.cbas.models;

import java.util.UUID;

public record Method(
    UUID id, String methodUrl, String inputDefinition, String outputDefinition, String recordType) {

  // Corresponding table column names in database
  public static final String ID_COL = "id";
  public static final String METHOD_URL_COL = "method_url";
  public static final String INPUT_DEFINITION_COL = "input_definition";
  public static final String OUTPUT_DEFINITION_COL = "output_definition";
  public static final String RECORD_TYPE_COL = "record_type";
}
