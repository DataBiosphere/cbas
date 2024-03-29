package bio.terra.cbas.runsets.results;

import org.springframework.http.HttpStatus;

public enum RunCompletionResult {
  SUCCESS("UPDATED_SUCCESSFULLY"),
  ERROR("RETRIABLE_ERROR"),
  VALIDATION_ERROR("VALIDATION_ERROR");
  private final String value;

  RunCompletionResult(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public HttpStatus toHttpStatus() {
    if (SUCCESS.toString().equalsIgnoreCase(this.value)) {
      return HttpStatus.OK;
    } else if (VALIDATION_ERROR.toString().equalsIgnoreCase(this.value)) {
      return HttpStatus.BAD_REQUEST;
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
