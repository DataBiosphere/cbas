package bio.terra.cbas.controllers.util;

import bio.terra.cbas.models.RunSet;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(AsyncExceptionHandler.class);

  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    String message = "";

    for (Object o : params) {
      if (o instanceof RunSet runSet) {
        message =
            "Exception thrown in Thread '%s' while executing method '%s' for Run Set '%s'. Error message: %s"
                .formatted(
                    Thread.currentThread().getName(),
                    method.getName(),
                    runSet.runSetId(),
                    ex.getMessage());
      }
    }

    if (message.isEmpty()) {
      message =
          "Exception thrown in Thread '%s' while executing method '%s'. Error message: %s"
              .formatted(Thread.currentThread().getName(), method.getName(), ex.getMessage());
    }

    logger.error(message);
  }
}
