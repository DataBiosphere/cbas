package bio.terra.cbas.dependencies.wds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestScheduledWdsClientRefresher {

  @Test
  void canGetCurrentClient() {
    ScheduledWdsClientRefresher scheduledWdsClientRefresher = new ScheduledWdsClientRefresher();
    assert (scheduledWdsClientRefresher.getCurrentClient() != null);
  }

  @Test
  void buildsUpRetiredClients() {
    ScheduledWdsClientRefresher scheduledWdsClientRefresher = new ScheduledWdsClientRefresher();

    List<Integer> clientHashes = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      clientHashes.add(scheduledWdsClientRefresher.getCurrentClient().hashCode());
      scheduledWdsClientRefresher.refreshClient();
    }

    assert (clientHashes.size() == 10);
    assertEquals(
        clientHashes,
        scheduledWdsClientRefresher.getRetiredClients().stream().map(Object::hashCode).toList());
  }

  @Test
  void movesRetiredClientsToExpired() {
    ScheduledWdsClientRefresher scheduledWdsClientRefresher = new ScheduledWdsClientRefresher();

    List<Integer> clientHashes = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      clientHashes.add(scheduledWdsClientRefresher.getCurrentClient().hashCode());
      scheduledWdsClientRefresher.refreshClient();
    }

    assertEquals(0, scheduledWdsClientRefresher.getExpiredClients().size());
    scheduledWdsClientRefresher.closeOldClients();
    assertEquals(
        clientHashes,
        scheduledWdsClientRefresher.getExpiredClients().stream().map(Object::hashCode).toList());

    assertEquals(0, scheduledWdsClientRefresher.getRetiredClients().size());
  }

  @Test
  void callsCloseOnExpiredClients() {
    ScheduledWdsClientRefresher scheduledWdsClientRefresher = new ScheduledWdsClientRefresher();

    // Prestage a set of retired and expired mock clients:
    List<Client> mockRetiredClients = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Client mockClient = mock(Client.class);
      // Note: Here, we do not stub out the close method.
      // MockitoExtension will automatically verify that unstubbed methods are NOT called.
      mockRetiredClients.add(mockClient);
      scheduledWdsClientRefresher.getRetiredClients().add(mockClient);
    }

    for (int i = 0; i < 10; i++) {
      Client mockClient = mock(Client.class);
      // Note:Here, we stub out the close method.
      // MockitoExtension will automatically verify that stubbed methods ARE called.
      doNothing().when(mockClient).close();
      scheduledWdsClientRefresher.getExpiredClients().add(mockClient);
    }

    // Perform the action:
    scheduledWdsClientRefresher.closeOldClients();

    assertEquals(0, scheduledWdsClientRefresher.getRetiredClients().size());
    assertEquals(mockRetiredClients, scheduledWdsClientRefresher.getExpiredClients());
  }
}
