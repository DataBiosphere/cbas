package bio.terra.cbas.dependencies.wes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cromwell.client.model.FailureMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TestCromwellService {

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

    String failureMessage = "aaaaaaa";
    String causedByMessage = "bbbbbbb";

    List<FailureMessage> input =
        List.of(
            new FailureMessage()
                .message(failureMessage)
                .causedBy(List.of(new FailureMessage().message(causedByMessage))));

    String expected = "aaaaaaa (bbbbbbb)";
    String actual = CromwellService.getErrorMessage(input);

    assertEquals(expected, actual);
  }

  @Test
  void oneFailureLongerThan100AndOneCausedBy() {

    String failureMessage =
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 110 as
    String causedByMessage = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"; // 40 bs

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
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 80 as
    String causedByMessage = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"; // 40 bs

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

  @Test
  void combinedCharLengthReturnsCorrectMessage() {

    List<FailureMessage> input =
        List.of(
            new FailureMessage()
                .message(String.valueOf(new char[80]).replace('\0', 'a'))
                .causedBy(
                    List.of(
                        new FailureMessage()
                            .message(String.valueOf(new char[25]).replace('\0', 'b')))));

    String expectedChars = new String(new char[97]);
    String expected = expectedChars + "...";
  }

  // null
  // empty array
  // more than 100 characters
}
