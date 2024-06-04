package bio.terra.cbas.config;

import bio.terra.cbas.controllers.util.AsyncBardExceptionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync()
@ConfigurationProperties(prefix = "cbas.async.bard")
public class AsyncBardConfiguration {
  private final int coreThreadPoolSize;
  private final int maxThreadPoolSize;
  private final int queueCapacity;

  public AsyncBardConfiguration(int coreThreadPoolSize, int maxThreadPoolSize, int queueCapacity) {
    this.coreThreadPoolSize = coreThreadPoolSize;
    this.maxThreadPoolSize = maxThreadPoolSize;
    this.queueCapacity = queueCapacity;
  }

  @Bean("bardAsyncExecutor")
  public ThreadPoolTaskExecutor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(coreThreadPoolSize);
    executor.setMaxPoolSize(maxThreadPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("BardLogExecutor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncBardExceptionHandler();
  }
}
