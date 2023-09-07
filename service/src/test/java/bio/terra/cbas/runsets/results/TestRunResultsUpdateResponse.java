package bio.terra.cbas.runsets.results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunResultsManager.class)
class TestRunResultsUpdateResponse {

  @Test
  void toHttpStatusSuccess() {
    assertHttpStatus(HttpStatus.OK, true, null);
  }

  @Test
  void toHttpStatusNoMessagesSuccess() {
    assertHttpStatus(HttpStatus.OK, true, "");
  }

  @Test
  void toHttpStatusErrorMessages() {
    assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, true, "error");
  }

  @Test
  void toHttpStatusNoSuccessErrorMessages() {
    assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, false, "error");
  }

  @Test
  void toHttpStatusNoSuccessNoErrorMessages() {
    assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, false, "");
  }

  private void assertHttpStatus(
      HttpStatus expectedHttpStatus, boolean responseSuccess, String responseErrorMessage) {
    RunResultsUpdateResponse response =
        new RunResultsUpdateResponse(responseSuccess, responseErrorMessage);
    HttpStatus httpStatus = response.toHttpStatus();
    // Validate the result
    assertEquals(expectedHttpStatus, httpStatus);
  }
}
