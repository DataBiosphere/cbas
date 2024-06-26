package bio.terra.cbas.config;

import bio.terra.cbas.controllers.util.AsyncExceptionHandler;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@ConfigurationProperties(prefix = "cbas.async.submission")
public class AsyncConfiguration implements AsyncConfigurer {
  private final int coreThreadPoolSize;
  private final int maxThreadPoolSize;
  private final int queueCapacity;

  @Autowired private RunDao runDao;
  @Autowired private RunSetDao runSetDao;

  public AsyncConfiguration(int coreThreadPoolSize, int maxThreadPoolSize, int queueCapacity) {
    this.coreThreadPoolSize = coreThreadPoolSize;
    this.maxThreadPoolSize = maxThreadPoolSize;
    this.queueCapacity = queueCapacity;
  }

  @Bean
  @Override
  public ThreadPoolTaskExecutor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(coreThreadPoolSize);
    executor.setMaxPoolSize(maxThreadPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("RunSetExecutor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncExceptionHandler(runDao, runSetDao);
  }
}
