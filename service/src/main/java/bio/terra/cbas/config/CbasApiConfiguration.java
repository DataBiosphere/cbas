package bio.terra.cbas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cbas.cbas-api")
public class CbasApiConfiguration {
  private int runSetsMaximumRecordIds;
  private int maxWorkflowInputs;
  private int maxWorkflowOutputs;
  private int maxSmartPollRunUpdateSeconds;
  private int maxSmartPollRunSetUpdateSeconds;
  private int minSecondsBetweenRunStatusPolls;

  public void setRunSetsMaximumRecordIds(int runSetsMaximumRecordIds) {
    this.runSetsMaximumRecordIds = runSetsMaximumRecordIds;
  }

  public int getRunSetsMaximumRecordIds() {
    return this.runSetsMaximumRecordIds;
  }

  public void setMaxWorkflowInputs(int maxWorkflowInputs) {
    this.maxWorkflowInputs = maxWorkflowInputs;
  }

  public int getMaxWorkflowInputs() {
    return this.maxWorkflowInputs;
  }

  public void setMaxWorkflowOutputs(int maxWorkflowOutputs) {
    this.maxWorkflowOutputs = maxWorkflowOutputs;
  }

  public int getMaxWorkflowOutputs() {
    return this.maxWorkflowOutputs;
  }

  public int getMaxSmartPollRunUpdateSeconds() {
    return maxSmartPollRunUpdateSeconds;
  }

  public void setMaxSmartPollRunUpdateSeconds(int maxSmartPollRunUpdateSeconds) {
    this.maxSmartPollRunUpdateSeconds = maxSmartPollRunUpdateSeconds;
  }

  public int getMaxSmartPollRunSetUpdateSeconds() {
    return maxSmartPollRunSetUpdateSeconds;
  }

  public void setMaxSmartPollRunSetUpdateSeconds(int maxSmartPollRunSetUpdateSeconds) {
    this.maxSmartPollRunSetUpdateSeconds = maxSmartPollRunSetUpdateSeconds;
  }

  public int getMinSecondsBetweenRunStatusPolls() {
    return minSecondsBetweenRunStatusPolls;
  }

  public void setMinSecondsBetweenRunStatusPolls(int minSecondsBetweenRunStatusPolls) {
    this.minSecondsBetweenRunStatusPolls = minSecondsBetweenRunStatusPolls;
  }
}
