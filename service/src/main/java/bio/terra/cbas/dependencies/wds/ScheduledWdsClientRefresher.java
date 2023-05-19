package bio.terra.cbas.dependencies.wds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.ws.rs.client.Client;
import org.databiosphere.workspacedata.client.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledWdsClientRefresher {

  private final Object synchronizer = new Object();

  private final Logger logger = LoggerFactory.getLogger(ScheduledWdsClientRefresher.class);

  private final List<Client> retiredClients;
  private final List<Client> expiredClients;

  private Client currentClient;

  private final String clientName = "wds";

  private final Callable<Client> clientFactory = () -> new ApiClient().getHttpClient();

  public ScheduledWdsClientRefresher() {
    this.retiredClients = new ArrayList<>();
    this.expiredClients = new ArrayList<>();
    refreshClient();
  }

  @Scheduled(
      fixedDelayString = "${cbas.scheduler.httpClientRefreshSeconds}",
      timeUnit = java.util.concurrent.TimeUnit.SECONDS)
  public void refreshClient() {
    synchronized (synchronizer) {
      logger.debug("Refreshing client for {} and marking current client as retired.", clientName);
      if (currentClient != null) {
        retiredClients.add(currentClient);
      }
    }
    try {
      currentClient = clientFactory.call();
    } catch (Exception ex) {
      logger.error("Error creating client for {}", clientName, ex);
    }
  }

  @Scheduled(
      fixedDelayString = "${cbas.scheduler.httpClientRefreshSeconds}",
      timeUnit = java.util.concurrent.TimeUnit.SECONDS)
  public void closeOldClients() {
    synchronized (synchronizer) {
      logger.debug(
          "Closing {} expired client(s) and marking {} retired clients as now expired for {}",
          expiredClients.size(),
          retiredClients.size(),
          clientName);
      for (Client client : expiredClients) {
        if (client != null) {
          client.close();
        } else {
          logger.warn("Programmer error: Null client in expired list for {}", clientName);
        }
      }
      // Clear out the expired clients and move the retired clients to the expired list to be
      // closed down next cycle:
      expiredClients.clear();
      expiredClients.addAll(retiredClients);
      retiredClients.clear();
    }
  }

  public Client getCurrentClient() {
    return currentClient;
  }

  protected List<Client> getRetiredClients() {
    return retiredClients;
  }

  protected List<Client> getExpiredClients() {
    return expiredClients;
  }
}
