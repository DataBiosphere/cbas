package bio.terra.cbas.common;

import java.time.OffsetDateTime;
import java.util.Date;

public final class DateUtils {

  private DateUtils() {}

  public static Date convertToDate(OffsetDateTime submissionTimestamp) {
    if (submissionTimestamp != null) {
      //      System.out.println("###### Offset timestamp: %s".formatted(submissionTimestamp));
      //
      //      ZonedDateTime offsetTime =
      //          OffsetDateTime.parse(submissionTimestamp.toString()).toZonedDateTime();
      //
      //      System.out.println("###### Zoned timestamp: %s".formatted(offsetTime));
      //
      //      ZoneOffset offsetValue = submissionTimestamp.getOffset();
      //
      //      System.out.println("###### Zoned offset: %s".formatted(offsetValue));
      //
      //
      //      System.out.println("###### Date timestamp: %s".formatted(date));
      //
      return Date.from(submissionTimestamp.toInstant());

      //      return new Date(offsetTime.toInstant().toEpochMilli());

      //      return new Date(submissionTimestamp.toInstant().toEpochMilli());
    }

    return null;
  }
}
