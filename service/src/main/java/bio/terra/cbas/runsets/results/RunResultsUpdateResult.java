package bio.terra.cbas.runsets.results;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;

public enum RunResultsUpdateResult {
  SUCCESS("UPDATED_SUCCESSFULLY"),
  ERROR("RETRIABLE_ERROR"),
  FATAL("FATAL_ERROR");
  private final String value;

  RunResultsUpdateResult(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public HttpStatus toHttpStatus() {
    if (SUCCESS.toString().equalsIgnoreCase(this.value)) {
      return HttpStatus.OK;
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
