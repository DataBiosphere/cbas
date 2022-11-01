package bio.terra.cbas.runsets.inputs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.databiosphere.workspacedata.model.RecordResponse;

public final class StockWdsRecordResponses {
  private StockWdsRecordResponses() {}

  static ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static RecordResponse emptyRecord() throws JsonProcessingException {
    return objectMapper.readValue(
        """
        {
          "id": "FOO1",
          "type": "FOO",
          "attributes": {
          }
        }"""
            .stripIndent()
            .trim(),
        RecordResponse.class);
  }

  public static RecordResponse wdsRecordWithFooRating(String rawAttributeJson)
      throws JsonProcessingException {
    return objectMapper.readValue(
        """
        {
          "id": "FOO1",
          "type": "FOO",
          "attributes": {
            "foo-rating": %s
          }
        }"""
            .formatted(rawAttributeJson)
            .stripIndent()
            .trim(),
        RecordResponse.class);
  }
}
