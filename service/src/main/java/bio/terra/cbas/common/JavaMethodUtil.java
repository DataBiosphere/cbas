package bio.terra.cbas.common;

public final class JavaMethodUtil {

  private JavaMethodUtil() {}

  /**
   * Traverses the call stack to find the calling method, and generate a suitable value for the
   * TAGKEY_NAME tag.
   *
   * @param extraStackDepth How many additional stack frames to go down before identifying the
   *     method. 0 is the direct caller's method name. 1 would be the caller's caller, and so on.
   * @return The name of the calling method.
   */
  public static String loggableMethodName(long extraStackDepth) {
    StackWalker.StackFrame caller =
        StackWalker.getInstance()
            .walk(stream -> stream.skip(1L + extraStackDepth).findFirst().get());

    return String.format("%s.%s", caller.getClassName(), caller.getMethodName());
  }
}
