package bio.terra.cbas.dependencies.wds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public class WdsClientSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(WdsClientSupport.class);

  // ProcessingException is thrown when the connection through the Relay/Listener has been RESET.
  @Retryable(
      include = {Exception.class},
      maxAttempts = 2,
      backoff = @Backoff(delay = 2000),
      listeners = {"retryLoggingListener"})
  public static <T> T withListenerResetRetry(WdsFunction<T> wdsFunction, String loggerHint)
      throws Exception {
    LOGGER.debug("Sending {} request to WDS ...", loggerHint);
    T functionResult = wdsFunction.run();
    LOGGER.debug("{} request successful", loggerHint);
    return functionResult;
  }

  /**
   * interface representing a callable Sam client function that returns a value.
   *
   * @param <T> return type of the Sam client function
   */
  @FunctionalInterface
  public interface WdsFunction<T> {
    T run() throws Exception;
  }
}
