package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@Testcontainers
class TestRunSetDao {

  @Autowired RunSetDao runSetDao;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;

  @Container
  static JdbcDatabaseContainer postgres =
      new PostgreSQLContainer("postgres:14")
          .withDatabaseName("test_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.jdbc-url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @BeforeAll
  static void setup() {
    postgres.start();
  }

  @BeforeEach
  void init() {
    methodDao.createMethod(method);
    methodVersionDao.createMethodVersion(methodVersion);
    runSetDao.createRunSet(runSet);
  }

  @AfterEach
  void cleanupDb() throws SQLException {
    DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .createStatement()
        .execute("DELETE FROM run_set; DELETE FROM method_version; DELETE FROM method;");
  }

  private final UUID workspaceId = UUID.randomUUID();

  Method method =
      new Method(
          UUID.fromString("00000000-0000-0000-0000-000000000008"),
          "fetch_sra_to_bam",
          "fetch_sra_to_bam",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "Github",
          workspaceId);

  MethodVersion methodVersion =
      new MethodVersion(
          UUID.fromString("80000000-0000-0000-0000-000000000008"),
          method,
          "1.0",
          "fetch_sra_to_bam sample submission",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl",
          workspaceId,
          "develop");

  RunSet runSet =
      new RunSet(
          UUID.fromString("10000000-0000-0000-0000-000000000008"),
          methodVersion,
          "fetch_sra_to_bam workflow",
          "fetch_sra_to_bam sample submission",
          false,
          true,
          CbasRunSetStatus.COMPLETE,
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          0,
          0,
          "[]",
          "[]",
          "sample",
          "user-foo",
          workspaceId);

  @Test
  void retrievesSingleRunSet() {
    RunSet actual = runSetDao.getRunSet(UUID.fromString("10000000-0000-0000-0000-000000000008"));

    assertEquals(runSet.runSetId(), actual.runSetId());
    assertEquals(
        runSet.methodVersion().methodVersionId(), actual.methodVersion().methodVersionId());
    assertEquals(runSet.name(), actual.name());
    assertEquals(runSet.description(), actual.description());
    assertEquals(runSet.status(), actual.status());
    assertEquals(runSet.runCount(), actual.runCount());
    assertEquals(runSet.errorCount(), actual.errorCount());
    assertEquals(runSet.recordType(), actual.recordType());
  }

  @Test
  void retrievesAllRunSets() {
    // DB has no non-templated run sets, so it will return empty list
    List<RunSet> runSets = runSetDao.getRunSets(null, false);
    assertEquals(0, runSets.size());

    List<RunSet> templateRunSets = runSetDao.getRunSets(2, true);
    assertEquals(1, templateRunSets.size());
  }

  @Test
  void getLatestRunSetWithMethodId() {
    // initially there is only one run set associated with this method
    assertEquals(
        runSet.runSetId(), runSetDao.getLatestRunSetWithMethodId(method.methodId()).runSetId());

    // now create a run set, submitted a day later, and confirm that it's retrieved instead.
    // this later run set has isTemplate=false, demonstrating that getLatestRunSetWithMethodId
    // is indifferent to a run set's isTemplate value.
    UUID laterRunSetId = UUID.fromString("10000000-0000-0000-0000-000000000007");
    runSetDao.createRunSet(
        new RunSet(
            laterRunSetId,
            methodVersion,
            "fetch_sra_to_bam workflow",
            "fetch_sra_to_bam sample submission",
            false,
            false,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.parse("2023-01-28T19:21:24.563932Z"),
            OffsetDateTime.parse("2023-01-28T19:21:24.563932Z"),
            OffsetDateTime.parse("2023-01-28T19:21:24.563932Z"),
            0,
            0,
            "[]",
            "[]",
            "sample",
            "user-foo",
            workspaceId));

    assertEquals(
        laterRunSetId, runSetDao.getLatestRunSetWithMethodId(method.methodId()).runSetId());
  }
}
