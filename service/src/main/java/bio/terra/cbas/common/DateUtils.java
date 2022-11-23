package bio.terra.cbas.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public final class DateUtils {

  private DateUtils() {}

  // helper function to ensure that all timestamps stored in database are in UTC
  public static OffsetDateTime currentTimeInUTC() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  public static Date convertToDate(OffsetDateTime submissionTimestamp) {
    if (submissionTimestamp != null) {
      return new Date(submissionTimestamp.toInstant().toEpochMilli());
    }

    return null;
  }
}
