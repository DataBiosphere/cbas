package bio.terra.cbas.initialization.cloneRecovery;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.RunSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CloneRecoveryService {

  private final Logger logger = LoggerFactory.getLogger(CloneRecoveryService.class);
  private final RunSetDao runSetDao;
  private final MethodDao methodDao;
  private final CloneRecoveryTransactionService transactionService;
  private final CbasContextConfiguration cbasContextConfig;

  public CloneRecoveryService(
      RunSetDao runSetDao,
      MethodDao methodDao,
      CloneRecoveryTransactionService transactionService,
      CbasContextConfiguration cbasContextConfig) {
    this.runSetDao = runSetDao;
    this.methodDao = methodDao;
    this.transactionService = transactionService;
    this.cbasContextConfig = cbasContextConfig;
  }

  public void cloneRecovery() {
    logger.info(
        "Starting clone recovery (workspaceId: {}, workspaceCreatedDate: {})",
        cbasContextConfig.getWorkspaceId(),
        cbasContextConfig.getWorkspaceCreatedDate());

    List<Method> clonedMethods =
        methodDao.getMethods().stream().filter(this::isMethodCloned).toList();

    List<MethodTemplateUpdateManifest> templateUpdateManifests =
        clonedMethods.stream()
            .map(m -> runSetDao.getRunSetsWithMethodId(m.methodId()))
            .map(this::generateTemplateUpdateManifest)
            .toList();

    templateUpdateManifests.forEach(
        manifest -> manifest.keepAsTemplate.forEach(transactionService::convertToTemplate));
    templateUpdateManifests.forEach(
        manifest -> manifest.toBeDeleted.forEach(transactionService::deleteRunSetAndRuns));
  }

  public record MethodTemplateUpdateManifest(
      List<RunSet> keepAsTemplate, List<RunSet> toBeDeleted) {}
  ;

  public MethodTemplateUpdateManifest generateTemplateUpdateManifest(List<RunSet> methodRunSets) {
    List<RunSet> methodClonedRunSets =
        methodRunSets.stream()
            .sorted(Comparator.comparing(RunSet::submissionTimestamp).reversed()) // latest first
            .filter(this::isRunSetCloned)
            .toList();

    if (methodClonedRunSets.isEmpty()) {
      return new MethodTemplateUpdateManifest(Collections.emptyList(), Collections.emptyList());
    } else {
      return new MethodTemplateUpdateManifest(
          List.of(methodClonedRunSets.get(0)), methodClonedRunSets.stream().skip(1).toList());
    }
  }

  public Boolean isMethodCloned(Method m) {
    return !m.originalWorkspaceId().equals(cbasContextConfig.getWorkspaceId());
  }

  public Boolean isRunSetCloned(RunSet r) {
    return !r.originalWorkspaceId().equals(cbasContextConfig.getWorkspaceId());
  }
}
