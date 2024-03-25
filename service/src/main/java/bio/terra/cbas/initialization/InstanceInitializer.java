package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.config.CbasInitializationConfiguration;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.util.BackfillOriginalWorkspaceIdService;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class InstanceInitializer implements ApplicationListener<ContextRefreshedEvent> {
  private final Logger logger = LoggerFactory.getLogger(InstanceInitializer.class);
  private final BackfillOriginalWorkspaceIdService backfillOriginalWorkspaceIdService;
  private final CloneRecoveryService cloneRecoveryService;
  private final CbasInitializationConfiguration cbasInitializationConfiguration;
  private final CbasContextConfiguration cbasContextConfiguration;

  public InstanceInitializer(
      CbasInitializationConfiguration cbasInitializationConfiguration,
      CbasContextConfiguration cbasContextConfiguration,
      BackfillOriginalWorkspaceIdService backfillOriginalWorkspaceIdService,
      CloneRecoveryService cloneRecoveryService) {
    this.cbasInitializationConfiguration = cbasInitializationConfiguration;
    this.cbasContextConfiguration = cbasContextConfiguration;
    this.backfillOriginalWorkspaceIdService = backfillOriginalWorkspaceIdService;
    this.cloneRecoveryService = cloneRecoveryService;
  }

  @Override
  public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
    if (cbasInitializationConfiguration.getEnabled()) {
      backfillAndValidate();
      cloneRecoveryService.cloneRecovery();
    }
  }

  public void backfillAndValidate() {
    backfillOriginalWorkspaceIdService.backfillAll();

    List<RunSet> unfilledRunSets = backfillOriginalWorkspaceIdService.getRunSetsToBackfill();
    List<Method> unfilledMethods = backfillOriginalWorkspaceIdService.getMethodsToBackfill();
    List<MethodVersion> unfilledMethodVersions =
        backfillOriginalWorkspaceIdService.getMethodVersionsToBackfill();

    if (unfilledRunSets.isEmpty()
        && unfilledMethods.isEmpty()
        && unfilledMethodVersions.isEmpty()) {

      logger.info(
          "Workspace {} has fully backfilled originalWorkspaceIds for RunSets, Methods, or MethodVersions",
          cbasContextConfiguration.getWorkspaceId());
    } else {
      throw new InitializationException(
          "Initialization failed because backfillOriginalWorkspaceIdService failed to backfill all RunSets, Methods, or Method Versions.");
    }
  }

  public class InitializationException extends RuntimeException {
    public InitializationException(String message) {
      super(message);
    }
  }
}
