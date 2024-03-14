package bio.terra.cbas.initialization;

import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InstanceInitializerBeanConfig {

  @Bean
  public BackfillOriginalWorkspaceIdBean backfillOriginalWorkspaceIdBean(
      RunSetDao runSetDao,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      CbasContextConfiguration cbasContextConfig) {
    return new BackfillOriginalWorkspaceIdBean(
        runSetDao, methodDao, methodVersionDao, cbasContextConfig);
  }
}
