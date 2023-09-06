package bio.terra.cbas.runsets.results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = RunResultsManager.class)
public class TestRunResultsUpdateResponse {

  @Test
  void toHttpStatusSuccess() {
    RunResultsUpdateResponse response = new RunResultsUpdateResponse(true, null);
    HttpStatus httpStatus = response.toHttpStatus();
    // Validate the result
    assertEquals(HttpStatus.OK, httpStatus);
  }

  @Test
  void toHttpStatusNoMessagesSuccess() {
    RunResultsUpdateResponse response = new RunResultsUpdateResponse(true, "");
    HttpStatus httpStatus = response.toHttpStatus();
    // Validate the result
    assertEquals(HttpStatus.OK, httpStatus);
  }

  @Test
  void toHttpStatusErrorMessages() {
    RunResultsUpdateResponse response = new RunResultsUpdateResponse(true, "error");
    HttpStatus httpStatus = response.toHttpStatus();
    // Validate the result
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpStatus);
  }

  @Test
  void toHttpStatusNoSuccessErrorMessages() {
    RunResultsUpdateResponse response = new RunResultsUpdateResponse(false, "error");
    HttpStatus httpStatus = response.toHttpStatus();
    // Validate the result
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpStatus);
  }

  @Test
  void toHttpStatusNoSuccessNoErrorMessages() {
    RunResultsUpdateResponse response = new RunResultsUpdateResponse(false, "");
    HttpStatus httpStatus = response.toHttpStatus();
    // Validate the result
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpStatus);
  }
}
