package bio.terra.cbas.config;

import bio.terra.cbas.dependencies.leonardo.LeonardoServiceApiException;
import bio.terra.cbas.dependencies.wds.WdsServiceApiException;
import bio.terra.common.exception.InternalServerErrorException;
import java.net.SocketTimeoutException;
import javax.ws.rs.ProcessingException;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestRetryConfig {
  private final RetryConfig retryConfig = new RetryConfig();

  @Test
  void returnsTrueForNestedSocketExceptionFromLeo() {
    Exception nestedException =
        new LeonardoServiceApiException(
            new ApiException(new SocketTimeoutException("mock socket timeout exception")));
    Assertions.assertTrue(retryConfig.isCausedBy(nestedException, retryConfig.retryableExceptions));
  }

  @Test
  void returnsTrueForNestedSocketExceptionFromWds() {
    Exception nestedException =
        new WdsServiceApiException(
            new org.databiosphere.workspacedata.client.ApiException(
                new SocketTimeoutException("mock socket timeout exception")));
    Assertions.assertTrue(retryConfig.isCausedBy(nestedException, retryConfig.retryableExceptions));
  }

  @Test
  void returnsTrueForProcessingException() {
    Exception exception = new ProcessingException("mock processing exception");
    Assertions.assertTrue(retryConfig.isCausedBy(exception, retryConfig.retryableExceptions));
  }

  @Test
  void returnsFalse() {
    Exception nestedException =
        new LeonardoServiceApiException(
            new ApiException(new InternalServerErrorException("mock internal server error")));
    Assertions.assertFalse(
        retryConfig.isCausedBy(nestedException, retryConfig.retryableExceptions));
  }
}
