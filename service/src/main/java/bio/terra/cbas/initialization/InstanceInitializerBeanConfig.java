package bio.terra.cbas.initialization;

import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InstanceInitializerBeanConfig {

  @Bean
  public CloneRecoveryBean cloneRecoveryBean(RunSetDao runSetDao, RunDao runDao) {
    return new CloneRecoveryBean(runSetDao, runDao);
  }
}
