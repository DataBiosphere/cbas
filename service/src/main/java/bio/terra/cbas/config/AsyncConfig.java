package bio.terra.cbas.config;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nonnull;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
@EnableAsync
public class AsyncConfig {

  static class ContextCopyingDecorator implements TaskDecorator {
    @Nonnull
    @Override
    public Runnable decorate(@Nonnull Runnable runnable) {
      RequestAttributes context = RequestContextHolder.currentRequestAttributes();
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      return () -> {
        try {
          RequestContextHolder.setRequestAttributes(context);
          MDC.setContextMap(contextMap);
          runnable.run();
        } finally {
          MDC.clear();
          RequestContextHolder.resetRequestAttributes();
        }
      };
    }
  }

  @Bean
  public TaskExecutor runSetExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("RunSetExecutor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setTaskDecorator(new ContextCopyingDecorator());
    executor.initialize();
    return executor;
  }
}
