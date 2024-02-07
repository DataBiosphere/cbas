package bio.terra.cbas.config;

import bio.terra.cbas.dependencies.leonardo.LeonardoServiceSocketTimeoutException;
import bio.terra.cbas.retry.RetryLoggingListener;
import java.net.SocketTimeoutException;
import javax.ws.rs.ProcessingException;
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
          if (exception instanceof ProcessingException
              || exception instanceof SocketTimeoutException || exception instanceof LeonardoServiceSocketTimeoutException) {
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
}
