package bio.terra.cbas.dependencies.wes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cromwell.client.model.FailureMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestCromwellService {

  @Test
  void oneFailureNoCausedBy() {

    List<FailureMessage> input = List.of(new FailureMessage().message("Fail message"));
    String expected = "Fail message";

    String actual = CromwellService.getErrorMessage(input);
    assertEquals(expected, actual);
  }

  @Test
  void listFailMessagesIsNull() {
    List<FailureMessage> input = List.of(new FailureMessage());
    String expected = "";

    String actual = CromwellService.getErrorMessage(input);
    assertEquals(expected, actual);
  }

  @Test
  void charLengthMoreThanHundred() {
    List<FailureMessage> input =
        List.of(new FailureMessage().message(String.valueOf(new char[106])));
    String expectedChars = new String(new char[97]);
    String expected = expectedChars + "...";

    String actual = CromwellService.getErrorMessage(input);
    assertEquals(expected, actual);
  }

  @Test
  void oneFailureOneCausedByShorterThan100() {

    String failureMessage = "Workflow input processing failed";
    String causedByMessage = "Required workflow input 'wf_hello.hello.addressee' not specified";

    List<FailureMessage> input =
        List.of(
            new FailureMessage()
                .message(failureMessage)
                .causedBy(List.of(new FailureMessage().message(causedByMessage))));

    String expected =
        "Workflow input processing failed (Required workflow input 'wf_hello.hello.addressee' not specified)";
    String actual = CromwellService.getErrorMessage(input);

    assertEquals(expected, actual);
  }

  @Test
  void oneFailureLongerThan100AndOneCausedBy() {

    String failureMessage =
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 110 a's
    String causedByMessage = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"; // 40 b's

    List<FailureMessage> input =
        List.of(
            new FailureMessage()
                .message(failureMessage)
                .causedBy(List.of(new FailureMessage().message(causedByMessage))));

    String expected =
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa...";
    String actual = CromwellService.getErrorMessage(input);

    assertEquals(expected, actual);
  }

  @Test
  void oneFailureOneCausedByLongerThan100() {

    String failureMessage =
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 80
    // a's
    String causedByMessage = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"; // 40 b's

    List<FailureMessage> input =
        List.of(
            new FailureMessage()
                .message(failureMessage)
                .causedBy(List.of(new FailureMessage().message(causedByMessage))));

    String expected =
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa (bbbbbbbbbbbbbbb...";
    String actual = CromwellService.getErrorMessage(input);

    assertEquals(expected, actual);
  }
}
