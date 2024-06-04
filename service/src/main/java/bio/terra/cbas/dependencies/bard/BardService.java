package bio.terra.cbas.dependencies.bard;

import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.common.iam.BearerToken;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public interface BardService {

  void logRunSetEvent(
      RunSetRequest request,
      MethodVersion methodVersion,
      List<String> workflowIds,
      BearerToken userToken);

  @Async("bardAsyncExecutor")
  void logEvent(String eventName, Map<String, String> properties, BearerToken userToken);
}
