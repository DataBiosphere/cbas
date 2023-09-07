package bio.terra.cbas.dependencies.wds;

public class WdsClientUtils {

  /**
   * Given an ApiException message from the WDS client, attempt to parse a human-readable error out
   * of the formatted message. ApiException.getMessage() returns a String in the format: "Message:
   * %s%nHTTP response code: %s%nHTTP response body: %s%nHTTP response headers: %s" and we'd like to
   * get just that first %s, representing the actual message.
   *
   * @param excMsg the WDS client exception message
   * @return the human-readable string, or the identity if it could not be parsed
   */
  public static String extractErrorMessage(String excMsg) {
    if (excMsg == null) {
      return "null";
    }
    int endPos = excMsg.indexOf("HTTP response code: ");
    // if the exception message starts with "Message: ",
    // endPos will be at least 9 characters from the start
    if (endPos > 9 && excMsg.startsWith("Message: ")) {
      return excMsg.substring(9, endPos - 1);
    }
    return excMsg;
  }
}
