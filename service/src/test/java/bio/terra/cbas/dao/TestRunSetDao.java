package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.RunSet;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRunSetDao {

  RunSetDao runSetDao;

  @Test
  @Transactional // means that it is all or none for a logical unit of work
  void retrievesSingleRunSet() {
    UUID runSetId = UUID.randomUUID(); // if calling to postgres db, this should not be random

    // what is pulled from the db?
    RunSet expected = new RunSet(...);

    // what we get back from the method call
    RunSet actual = runSetDao.getRunSet(runSetId);

    assertEquals(expected, actual);
  }

  @Test
  void retrievesAllRunSets() {

    List<RunSet> runSets = runSetDao.getRunSets();

    // what is pulled from the db?
    List<RunSet> runSetsExpected = new RunSet(...)

    assertEquals(runSetsExpected, runSets);
  }
}
