package bio.terra.cbas.common;

import java.time.OffsetDateTime;
import java.util.Date;

public final class DateUtils {

  private DateUtils() {}

  public static Date convertToDate(OffsetDateTime submissionTimestamp) {
    if (submissionTimestamp != null) {
      return new Date(submissionTimestamp.toInstant().toEpochMilli());
    }

    return null;
  }
}
