package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRunSetDao {

  @Autowired RunSetDao runSetDao;
  UUID runSetId;
  MethodVersion methodVersion;

  @BeforeEach
  void setUp() {
    runSetId = UUID.randomUUID();
  }

  @Test
  void retrievesSingleRunSet() {
    String runSetName = "testRunSet";
    String runSetDescription = "testing a run set";
    Integer runCount = 1;
    Integer errorCount = 0;

    // what is pulled from the db?
    RunSet testRunSet =
        new RunSet(
            runSetId,
            methodVersion,
            runSetName,
            runSetDescription,
            Boolean.FALSE,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            runCount,
            errorCount,
            "input definition",
            "output definition",
            "FOO");

    // what we get back from the method call
    RunSet actual = runSetDao.getRunSet(runSetId);

    assertEquals(testRunSet, actual);
  }

  @Test
  void retrievesAllRunSets() {

    List<RunSet> runSets = runSetDao.getRunSets();

    // what is pulled from the db?
    // List<RunSet> runSetsExpected = new RunSet(...)

    // assertEquals(runSetsExpected, runSets);
  }
}
