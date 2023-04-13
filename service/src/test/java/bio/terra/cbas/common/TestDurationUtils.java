package bio.terra.cbas.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class TestDurationUtils {

  @Test
  void testSecondParsing() {
    String input = "5 seconds";
    assertEquals(Duration.ofSeconds(5), DurationUtils.durationFromString(input));
  }

  @Test
  void testMinuteParsing() {
    String input = "10 minutes";
    assertEquals(Duration.ofMinutes(10), DurationUtils.durationFromString(input));
  }

  @Test
  void testMilliParsing() {
    String input = "500 milliseconds";
    assertEquals(Duration.ofMillis(500), DurationUtils.durationFromString(input));
  }

  @Test
  void testCombinedParsing() {
    String input = "10 minutes 5s 500ms";
    assertEquals(
        Duration.ofMillis(10 * 60 * 1000 + 5 * 1000 + 500),
        DurationUtils.durationFromString(input));
  }

  @Test
  void testPartialNumbers() {
    String input = "5.5s";
    assertEquals(Duration.ofMillis(5500), DurationUtils.durationFromString(input));
  }

  @Test
  void testDurationFromString_ValidInputs() {
    assertEquals(Duration.ofMillis(5000), DurationUtils.durationFromString("5 seconds"));
    assertEquals(Duration.ofMillis(1500), DurationUtils.durationFromString("1.5 seconds"));
    assertEquals(Duration.ofMillis(5500), DurationUtils.durationFromString("5.5 seconds"));
    assertEquals(Duration.ofMillis(3750), DurationUtils.durationFromString("3.75 seconds"));
    assertEquals(Duration.ofMillis(250), DurationUtils.durationFromString("250 milliseconds"));
    assertEquals(Duration.ofMinutes(5), DurationUtils.durationFromString("5 minutes"));
    assertEquals(Duration.ofHours(2), DurationUtils.durationFromString("2 hours"));
    assertEquals(Duration.ofDays(3), DurationUtils.durationFromString("3 days"));
    assertEquals(Duration.ofMillis(1234), DurationUtils.durationFromString("1.234 seconds"));
    assertEquals(Duration.ofMillis(1234), DurationUtils.durationFromString("1.234 SECONDS"));
  }

  @Test
  void testDurationFromString_MultipleTimeUnits() {
    assertEquals(Duration.ofMillis(61000), DurationUtils.durationFromString("1 minute 1 second"));
    assertEquals(
        Duration.ofMillis(3665000), DurationUtils.durationFromString("1 hour 1 minute 5 seconds"));
    assertEquals(Duration.ofMinutes(90), DurationUtils.durationFromString("1.5 hours"));
    assertEquals(
        Duration.ofHours(3).plusMinutes(30),
        DurationUtils.durationFromString("3 hours 30 minutes"));
    assertEquals(
        Duration.ofMillis(1001), DurationUtils.durationFromString("1 second 1 millisecond"));
  }

  @Test
  public void testDurationFromStringWithEmptyInput() {
    assertThrows(IllegalArgumentException.class, () -> DurationUtils.durationFromString(""));
  }

  @Test
  public void testDurationFromStringWithNullInput() {
    assertThrows(IllegalArgumentException.class, () -> DurationUtils.durationFromString(null));
  }

  @Test
  public void testDurationFromStringWithInvalidFormats() {
    assertThrows(
        IllegalArgumentException.class, () -> DurationUtils.durationFromString("10 invalidunits"));
    assertThrows(
        IllegalArgumentException.class, () -> DurationUtils.durationFromString("invalid input"));

    assertThrows(
        IllegalArgumentException.class,
        () -> DurationUtils.durationFromString("10 seconds invalid invalid"));

    assertThrows(
        IllegalArgumentException.class,
        () -> DurationUtils.durationFromString("10 seconds invalid 20 seconds"));

    assertThrows(
        IllegalArgumentException.class,
        () -> DurationUtils.durationFromString("invalid 10 seconds"));
  }

  @Test
  public void testDurationFromStringWithInvalidUnitFormats() {
    assertThrows(IllegalArgumentException.class, () -> DurationUtils.durationFromString("5 sec"));
    assertThrows(IllegalArgumentException.class, () -> DurationUtils.durationFromString("10 min."));
    assertThrows(IllegalArgumentException.class, () -> DurationUtils.durationFromString("1 hr"));
    assertThrows(IllegalArgumentException.class, () -> DurationUtils.durationFromString("3 days."));
  }

  @Test
  public void testDurationFromStringWithInvalidCharactersOrExtraWhitespace() {
    assertThrows(
        IllegalArgumentException.class, () -> DurationUtils.durationFromString("5 seconds@"));
    assertThrows(
        IllegalArgumentException.class, () -> DurationUtils.durationFromString("5 days \t"));
  }

  @Test
  public void testDurationFromStringWithInvalidOrUnsupportedUnitTypes() {
    assertThrows(
        IllegalArgumentException.class, () -> DurationUtils.durationFromString("5 decades"));
    assertThrows(
        IllegalArgumentException.class, () -> DurationUtils.durationFromString("2 centuries"));
    assertThrows(
        IllegalArgumentException.class, () -> DurationUtils.durationFromString("3 bananas"));
  }
}
