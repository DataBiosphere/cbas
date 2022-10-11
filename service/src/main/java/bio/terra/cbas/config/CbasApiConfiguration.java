package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "cbas.cbas-api")
public class CbasApiConfiguration {
  int runSetsMaximumRecordIds;

  public CbasApiConfiguration(int runSetsMaximumRecordIds) {
    this.runSetsMaximumRecordIds = runSetsMaximumRecordIds;
  }

  public int getRunSetsMaximumRecordIds() {
    return this.runSetsMaximumRecordIds;
  }
}
