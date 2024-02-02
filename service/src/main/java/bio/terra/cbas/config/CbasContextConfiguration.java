package bio.terra.cbas.config;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cbas.context")
public class CbasContextConfiguration extends BaseDatabaseConfiguration {
  private String workspaceId;
  private String workspaceCreatedDate;

  public UUID getWorkspaceId() {
    return UUID.fromString(workspaceId);
  }

  public OffsetDateTime getWorkspaceCreatedDate() {
    return OffsetDateTime.parse(workspaceCreatedDate);
  }

  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  public void setWorkspaceCreatedDate(String createdDate) {
    this.workspaceCreatedDate = createdDate;
  }
}
