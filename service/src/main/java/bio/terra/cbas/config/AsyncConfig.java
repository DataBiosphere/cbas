package bio.terra.cbas.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  //  static class ContextCopyingDecorator implements TaskDecorator {
  //    @Nonnull
  //    @Override
  //    public Runnable decorate(@Nonnull Runnable runnable) {
  //      RequestAttributes context = RequestContextHolder.currentRequestAttributes();
  //      Map<String, String> contextMap = MDC.getCopyOfContextMap();
  //      return () -> {
  //        try {
  //          RequestContextHolder.setRequestAttributes(context);
  //          MDC.setContextMap(contextMap);
  //          runnable.run();
  //        } finally {
  //          MDC.clear();
  //          RequestContextHolder.resetRequestAttributes();
  //        }
  //      };
  //    }
  //  }

  @Bean
  public ThreadPoolTaskExecutor runSetExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("RunSetExecutor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
