package bio.terra.cbas.initialization;

import bio.terra.cbas.util.BackfillOriginalWorkspaceIdService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class InstanceInitializer implements ApplicationListener<ContextRefreshedEvent> {

  @Autowired BackfillOriginalWorkspaceIdService backfillOriginalWorkspaceIdService;

  public InstanceInitializer(
      BackfillOriginalWorkspaceIdService backfillOriginalWorkspaceIdService) {
    this.backfillOriginalWorkspaceIdService = backfillOriginalWorkspaceIdService;
  }

  @Override
  public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
    backfillOriginalWorkspaceIdService.backfillAll();
  }
}
