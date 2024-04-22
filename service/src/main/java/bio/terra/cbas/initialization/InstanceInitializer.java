package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasInitializationConfiguration;
import bio.terra.cbas.initialization.cloneRecovery.CloneRecoveryService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class InstanceInitializer implements ApplicationListener<ContextRefreshedEvent> {
  private final CloneRecoveryService cloneRecoveryService;
  private final CbasInitializationConfiguration cbasInitializationConfiguration;

  public InstanceInitializer(
      CbasInitializationConfiguration cbasInitializationConfiguration,
      CloneRecoveryService cloneRecoveryService) {
    this.cbasInitializationConfiguration = cbasInitializationConfiguration;
    this.cloneRecoveryService = cloneRecoveryService;
  }

  @Override
  public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
    if (cbasInitializationConfiguration.getEnabled()) {
      cloneRecoveryService.cloneRecovery();
    }
  }

  public class InitializationException extends RuntimeException {
    public InitializationException(String message) {
      super(message);
    }
  }
}
