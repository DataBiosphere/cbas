package bio.terra.cbas.monitoring;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TimeLimitedUpdater {

  public record UpdateResult<A>(
      List<A> updatedList, int totalEligible, int totalUpdated, boolean fullyUpdated) {}

  public static <A> UpdateResult<A> update(
      List<A> inputList,
      Function<A, UUID> idExtractor,
      Function<A, Boolean> readyForUpdate,
      Comparator<A> updateOrderComparator,
      Function<A, A> updateOperation,
      OffsetDateTime endTime) {

    // Linked hash map allows random access for updates _and_ preserves input order
    LinkedHashMap<UUID, A> idIndexedInputs =
        inputList.stream()
            .collect(
                Collectors.toMap(
                    idExtractor,
                    a -> a,
                    (u, v) -> {
                      throw new IllegalStateException(String.format("Duplicate key %s", u));
                    },
                    LinkedHashMap::new));

    // This re-sorted list gives us the order in which to update the inputs:
    Iterator<UUID> updateIterator =
        inputList.stream().sorted(updateOrderComparator).map(idExtractor).iterator();

    // Tracker numbers:
    int totalEligible = 0;
    int totalUpdated = 0;

    // Run the updates for as long as the end time is not reached:
    while (updateIterator.hasNext()) {
      UUID idToUpdate = updateIterator.next();
      A valueToUpdate = idIndexedInputs.get(idToUpdate);

      if (readyForUpdate.apply(valueToUpdate)) {
        totalEligible++;
        if (OffsetDateTime.now().isBefore(endTime)) {
          A updated = updateOperation.apply(idIndexedInputs.get(idToUpdate));
          idIndexedInputs.put(idToUpdate, updated);
          totalUpdated++;
        }
      }
    }

    return new UpdateResult<>(
        idIndexedInputs.values().stream().toList(),
        totalEligible,
        totalUpdated,
        totalUpdated == totalEligible);
  }
}
