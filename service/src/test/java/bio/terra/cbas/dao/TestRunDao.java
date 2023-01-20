package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.dao.RunDao.RunsFilters;
import bio.terra.cbas.dao.util.WhereClause;
import bio.terra.cbas.models.CbasRunStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestRunDao {

  // Sort the NON_TERMINAL_STATES so that we can guarantee the order (based on enum ordination):
  // UNKNOWN, QUEUED, INITIALIZING, RUNNING, PAUSED, CANCELING
  private static final List<CbasRunStatus> sortedNonTerminalStatuses =
      CbasRunStatus.NON_TERMINAL_STATES.stream().sorted().toList();

  @Test
  void emptyRunFilters() {
    RunsFilters filters = RunsFilters.empty();
    assertEquals(new WhereClause(List.of(), Map.of()), filters.buildWhereClause());
  }

  @Test
  void emptyRunFilters2() {
    RunsFilters filters = new RunsFilters(null, null);
    assertEquals(new WhereClause(List.of(), Map.of()), filters.buildWhereClause());
  }

  @Test
  void emptyRunFilters3() {
    RunsFilters filters = new RunsFilters(null, List.of());
    assertEquals(new WhereClause(List.of(), Map.of()), filters.buildWhereClause());
  }

  @Test
  void runSetId() {
    UUID uuid = UUID.randomUUID();
    RunsFilters filters = new RunsFilters(uuid, null);
    WhereClause actual = filters.buildWhereClause();
    assertEquals("WHERE (run.run_set_id = :runSetId)", actual.toString());
    assertEquals(Map.of("runSetId", uuid), actual.params());
  }

  @Test
  void filterSortedNonTerminalStates() {

    RunsFilters filters = new RunsFilters(null, sortedNonTerminalStatuses);
    WhereClause actual = filters.buildWhereClause();
    assertEquals(
        "WHERE (run.status in (:status_0,:status_1,:status_2,:status_3,:status_4,:status_5))",
        actual.toString());
    assertEquals(
        Map.of(
            "status_0", "UNKNOWN",
            "status_1", "QUEUED",
            "status_2", "INITIALIZING",
            "status_3", "RUNNING",
            "status_4", "PAUSED",
            "status_5", "CANCELING"),
        actual.params());
  }

  @Test
  void filterRawNonTerminalStates() {

    // Unsorted NON_TERMINAL_STATES is the more likely use case.
    RunsFilters filters = new RunsFilters(null, CbasRunStatus.NON_TERMINAL_STATES);
    WhereClause actual = filters.buildWhereClause();
    assertEquals(
        "WHERE (run.status in (:status_0,:status_1,:status_2,:status_3,:status_4,:status_5))",
        actual.toString());

    // We can't assert any ordering on which status ends up in which 'status_0'...'status_5'
    // placeholder,
    // but we can make sure all the right keys and values are in the map in SOME order...
    assertEquals(
        CbasRunStatus.NON_TERMINAL_STATES.stream()
            .map(CbasRunStatus::toString)
            .collect(Collectors.toSet()),
        new HashSet<>(actual.params().values()));
    assertEquals(
        Set.of("status_0", "status_1", "status_2", "status_3", "status_4", "status_5"),
        actual.params().keySet());
  }

  @Test
  void filterRunSetIdAndSortedNonTerminalStatuses() {
    UUID uuid = UUID.randomUUID();
    RunsFilters filters = new RunsFilters(uuid, sortedNonTerminalStatuses);
    WhereClause actual = filters.buildWhereClause();

    assertEquals(
        "WHERE (run.run_set_id = :runSetId) AND (run.status in (:status_0,:status_1,:status_2,:status_3,:status_4,:status_5))",
        actual.toString());
    assertEquals(
        Map.of(
            "runSetId", uuid,
            "status_0", "UNKNOWN",
            "status_1", "QUEUED",
            "status_2", "INITIALIZING",
            "status_3", "RUNNING",
            "status_4", "PAUSED",
            "status_5", "CANCELING"),
        actual.params());
  }

  //  @Test
  //  void createsNewRun() {
  //    UUID runId = UUID.randomUUID();
  //    String engineId = UUID.randomUUID().toString();
  //    OffsetDateTime time = OffsetDateTime.now();
  ////    RunSet runSet = new RunSet(
  ////
  ////    );
  //
  //    Run newRun = new Run(
  //        runId,
  //        engineId,
  //        runSet,
  //        "FOO1",
  //        time,
  //        CbasRunStatus.COMPLETE,
  //        time,
  //        time,
  //        null
  //    );
  //  }
}
