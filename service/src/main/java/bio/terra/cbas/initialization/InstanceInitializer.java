package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.config.CbasInitializationConfiguration;
import bio.terra.cbas.initialization.cloneRecovery.CloneRecoveryService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class InstanceInitializer implements ApplicationListener<ContextRefreshedEvent> {
  private final Logger logger = LoggerFactory.getLogger(InstanceInitializer.class);
  private final CloneRecoveryService cloneRecoveryService;
  private final CbasInitializationConfiguration cbasInitializationConfiguration;
  private final CbasContextConfiguration cbasContextConfiguration;

  public InstanceInitializer(
      CbasInitializationConfiguration cbasInitializationConfiguration,
      CbasContextConfiguration cbasContextConfiguration,
      CloneRecoveryService cloneRecoveryService) {
    this.cbasInitializationConfiguration = cbasInitializationConfiguration;
    this.cbasContextConfiguration = cbasContextConfiguration;
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
