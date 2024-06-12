package bio.terra.cbas.dependencies.bard;

import bio.terra.common.iam.BearerToken;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public interface BardService {

  @Async("bardAsyncExecutor")
  void logEvent(String eventName, Map<String, String> properties, BearerToken userToken);
}
