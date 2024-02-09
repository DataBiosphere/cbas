package bio.terra.cbas.config;

import bio.terra.cbas.retry.RetryLoggingListener;
import java.net.SocketTimeoutException;
import java.util.List;
import javax.ws.rs.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@EnableRetry
@Configuration
public class RetryConfig {
  protected final List<Class<? extends Throwable>> retryableExceptions =
      List.of(ProcessingException.class, SocketTimeoutException.class);

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Bean(name = "listenerResetRetryTemplate")
  public RetryTemplate listenerResetRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    // Fixed delay of 1 second between retries
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(1000L);

    // Inner retry (assumping the classifier hits): up to 3 times
    SimpleRetryPolicy srp = new SimpleRetryPolicy();
    srp.setMaxAttempts(3);

    ExceptionClassifierRetryPolicy ecrp = new ExceptionClassifierRetryPolicy();
    ecrp.setExceptionClassifier(
        exception -> {
          if (isCausedBy(exception, retryableExceptions)) {
            logger.info("*** FIND ME - Received exception %s - Exception is a retryable exception. Will retry ***".formatted(exception.getClass()));
            return srp;
          } else {
            return new NeverRetryPolicy();
          }
        });

    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    retryTemplate.setRetryPolicy(ecrp);
    retryTemplate.setThrowLastExceptionOnExhausted(true);
    retryTemplate.setListeners(new RetryListener[] {new RetryLoggingListener()});

    return retryTemplate;
  }

  // Recursive method to determine whether an Exception passed is, or has a cause, that is a
  // subclass or implementation of the Throwable(s) provided.
  public boolean isCausedBy(Throwable caught, List<Class<? extends Throwable>> isOfOrCausedByList) {
    if (caught == null) {
      return false;
    } else if (isOfOrCausedByList.stream().anyMatch(e -> e.isAssignableFrom(caught.getClass()))) {
      return true;
    } else {
      return isCausedBy(caught.getCause(), isOfOrCausedByList);
    }
  }
}
