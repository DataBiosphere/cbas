package bio.terra.cbas.config;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cbas.context")
public class CbasContextConfiguration extends BaseDatabaseConfiguration {
  private String workspaceId;

  public UUID getWorkspaceId() {
    return UUID.fromString(workspaceId);
  }

  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }
}
