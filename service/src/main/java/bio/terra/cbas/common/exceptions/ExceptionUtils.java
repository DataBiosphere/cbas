package bio.terra.cbas.common.exceptions;

public final class ExceptionUtils {

  private ExceptionUtils() {}

  public static String getSamForbiddenExceptionMsg(String samActionType, String samResourceType) {
    return "User doesn't have '%s' permission on '%s' resource"
        .formatted(samActionType, samResourceType);
  }
}
