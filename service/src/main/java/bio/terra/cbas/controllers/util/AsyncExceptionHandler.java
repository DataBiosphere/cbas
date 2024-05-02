package bio.terra.cbas.controllers.util;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(AsyncExceptionHandler.class);

  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    // handle exception
    logger.error(
        "Exception thrown in Thread '%s' while executing method '%s'. Error message: %s"
            .formatted(Thread.currentThread().getName(), method.getName(), ex.getMessage()));
  }
}
