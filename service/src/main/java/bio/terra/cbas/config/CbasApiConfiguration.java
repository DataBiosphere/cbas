package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cbas.cbas-api")
public class CbasApiConfiguration {
  private int runSetsMaximumRecordIds;

  public void setRunSetsMaximumRecordIds(int runSetsMaximumRecordIds) {
    this.runSetsMaximumRecordIds = runSetsMaximumRecordIds;
  }

  public int getRunSetsMaximumRecordIds() {
    return this.runSetsMaximumRecordIds;
  }
}
