package bio.terra.cbas.initialization;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class InstanceInitializer implements ApplicationListener<ContextRefreshedEvent> {

  private final BackfillOriginalWorkspaceIdBean backfillOriginalWorkspaceIdBean;

  public InstanceInitializer(BackfillOriginalWorkspaceIdBean backfillOriginalWorkspaceIdBean) {
    this.backfillOriginalWorkspaceIdBean = backfillOriginalWorkspaceIdBean;
  }

  @Override
  public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
    backfillOriginalWorkspaceIdBean.backfillOriginalWorkspaceIds();
  }
}
