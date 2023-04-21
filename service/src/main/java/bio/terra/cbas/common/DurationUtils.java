package bio.terra.cbas.common;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationUtils {

  private DurationUtils() {}

  private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(\\w+)");

  private static final Map<String, ChronoUnit> UNIT_MAP = new HashMap<>();

  static {
    UNIT_MAP.put("millisecond", ChronoUnit.MILLIS);
    UNIT_MAP.put("milliseconds", ChronoUnit.MILLIS);
    UNIT_MAP.put("ms", ChronoUnit.MILLIS);
    UNIT_MAP.put("second", ChronoUnit.SECONDS);
    UNIT_MAP.put("seconds", ChronoUnit.SECONDS);
    UNIT_MAP.put("s", ChronoUnit.SECONDS);
    UNIT_MAP.put("minute", ChronoUnit.MINUTES);
    UNIT_MAP.put("minutes", ChronoUnit.MINUTES);
    UNIT_MAP.put("m", ChronoUnit.MINUTES);
    UNIT_MAP.put("hour", ChronoUnit.HOURS);
    UNIT_MAP.put("hours", ChronoUnit.HOURS);
    UNIT_MAP.put("h", ChronoUnit.HOURS);
    UNIT_MAP.put("day", ChronoUnit.DAYS);
    UNIT_MAP.put("days", ChronoUnit.DAYS);
    UNIT_MAP.put("d", ChronoUnit.DAYS);
  }

  private static boolean isValidDurationString(String input) {
    String pattern = "^(\\d+(\\.\\d+)?[ ]*[a-zA-Z]+[ ]*)+$";
    return input.matches(pattern);
  }

  public static Duration durationFromString(String input) {
    if (input == null || !isValidDurationString(input)) {
      throw new IllegalArgumentException("Invalid duration string: " + input);
    }

    Matcher matcher = DURATION_PATTERN.matcher(input);
    Duration duration = Duration.ZERO;
    while (matcher.find()) {
      double amount = Double.parseDouble(matcher.group(1));
      String unitStr = matcher.group(3).toLowerCase();
      if (!UNIT_MAP.containsKey(unitStr)) {
        throw new IllegalArgumentException("Invalid time unit: " + unitStr);
      }
      ChronoUnit unit = UNIT_MAP.get(unitStr);
      duration = duration.plus(Duration.ofMillis((long) (amount * unit.getDuration().toMillis())));
    }
    return duration;
  }
}
