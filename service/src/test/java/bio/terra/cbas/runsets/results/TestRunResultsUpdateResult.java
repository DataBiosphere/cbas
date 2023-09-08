package bio.terra.cbas.runsets.results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunResultsUpdateResult.class)
class TestRunResultsUpdateResult {

  @Test
  void toHttpStatusSuccess() {
    assertHttpStatus(HttpStatus.OK, RunResultsUpdateResult.SUCCESS);
  }

  @Test
  void toHttpStatusErrorMessages() {
    assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, RunResultsUpdateResult.ERROR);
  }

  @Test
  void toHttpStatusNoSuccessErrorMessages() {
    assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, RunResultsUpdateResult.FATAL);
  }

  private void assertHttpStatus(HttpStatus expectedHttpStatus, RunResultsUpdateResult result) {
    HttpStatus httpStatus = result.toHttpStatus();
    // Validate the result
    assertEquals(expectedHttpStatus, httpStatus);
  }
}
