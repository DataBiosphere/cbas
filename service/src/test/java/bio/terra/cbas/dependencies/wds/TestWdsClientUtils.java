package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestWdsClientUtils {

  // successful parsing
  @Test
  void testExpectedWdsErrorFormat() {
    String input =
        "Message: the real stuff\nHTTP response code: 0\nHTTP response body: null\nHTTP response headers: null";
    String expected = "the real stuff";
    String actual = WdsClientUtils.extractErrorMessage(input);
    assertEquals(expected, actual);
  }

  // edge cases and exception cases
  @Test
  void testNullInput() {
    String input = null;
    String expected = "null";
    String actual = WdsClientUtils.extractErrorMessage(input);
    assertEquals(expected, actual);
  }

  @Test
  void testEmptyStringInput() {
    String input = "";
    String expected = "";
    String actual = WdsClientUtils.extractErrorMessage(input);
    assertEquals(expected, actual);
  }

  @Test
  void testSomeOtherErrorMessage() {
    String input = "How quickly daft jumping zebras vex!";
    String expected = "How quickly daft jumping zebras vex!";
    String actual = WdsClientUtils.extractErrorMessage(input);
    assertEquals(expected, actual);
  }

  @Test
  void testStartsOkButEndsBadly() {
    String input = "Message: something that doesn't include the expected response code string";
    String expected = "Message: something that doesn't include the expected response code string";
    String actual = WdsClientUtils.extractErrorMessage(input);
    assertEquals(expected, actual);
  }

  @Test
  void testAlmostTheWdsErrorFormat() {
    String input = "My Message: HTTP response codes are useful.";
    String expected = "My Message: HTTP response codes are useful.";
    String actual = WdsClientUtils.extractErrorMessage(input);
    assertEquals(expected, actual);
  }

}
