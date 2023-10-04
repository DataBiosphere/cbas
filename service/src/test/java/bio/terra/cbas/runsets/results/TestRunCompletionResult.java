package bio.terra.cbas.runsets.results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunCompletionResult.class)
class TestRunCompletionResult {

  @Test
  void toHttpStatusSuccess() {
    assertHttpStatus(HttpStatus.OK, RunCompletionResult.SUCCESS);
  }

  @Test
  void toHttpStatusErrorMessages() {
    assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, RunCompletionResult.ERROR);
  }

  @Test
  void toHttpStatusNoSuccessErrorMessages() {
    assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, RunCompletionResult.VALIDATION);
  }

  private void assertHttpStatus(HttpStatus expectedHttpStatus, RunCompletionResult result) {
    HttpStatus httpStatus = result.toHttpStatus();
    // Validate the result
    assertEquals(expectedHttpStatus, httpStatus);
  }
}
