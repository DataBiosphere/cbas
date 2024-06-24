package bio.terra.cbas.controllers.util;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

public class AsyncBardExceptionHandler implements AsyncUncaughtExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(AsyncBardExceptionHandler.class);

  private static final String STANDARD_LOG_MESSAGE =
      "Exception thrown in Thread '%s' while executing method '%s'. Error message: %s";

  public AsyncBardExceptionHandler() {}

  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    String methodName = method.getName();
    if (methodName.equals("logEvent")) {
      handleExceptionFromAsyncBardLog(ex, methodName, params);
    } else {
      String logMsg =
          STANDARD_LOG_MESSAGE.formatted(
              Thread.currentThread().getName(), method.getName(), ex.getMessage());
      logger.error(logMsg);
    }
  }

  public void handleExceptionFromAsyncBardLog(Throwable ex, String methodName, Object... params) {
    String eventName = params[0].toString();
    var logMsg =
        "Exception thrown in Thread '%s' while executing method '%s' for Bard Log '%s'. Error message: %s"
            .formatted(Thread.currentThread().getName(), methodName, eventName, ex.getMessage());
    logger.error(logMsg);
  }
}
