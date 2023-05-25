package bio.terra.cbas.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Component("retryLoggingListener")
public class RetryLoggingListener implements RetryListener {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public <T, E extends Throwable> void close(
      RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    if (context.getRetryCount() > 1) {
      logger.debug("Retryable method closing ({}th retry)", context.getRetryCount());
    }
  }

  @Override
  public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    if (context.getRetryCount() > 1) {
      logger.debug("Retryable method opening ({}th retry)", context.getRetryCount());
    }
    return true;
  }

  @Override
  public <T, E extends Throwable> void onError(
      RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    logger.warn(
        "Retryable method threw {}th exception {}", context.getRetryCount(), throwable);
  }
}
