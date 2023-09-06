package bio.terra.cbas.runsets.results;

import org.springframework.http.HttpStatus;

public record RunResultsUpdateResponse(boolean success, String errorMessages) {
  public HttpStatus toHttpStatus() {
    if (success == true && (errorMessages == null || errorMessages.isEmpty())) {
      return HttpStatus.OK;
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
