package bio.terra.cbas.dao;

// import bio.terra.cbas.initialization.InstanceInitializer;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
public class TestRunSetDaoContainerized {
  @Autowired RunSetDao runSetDao;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;
  // @MockBean private InstanceInitializer instanceInitializerMock;

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

  @AfterEach
  void cleanupDb() throws SQLException {
    DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .createStatement()
        .execute("DELETE FROM run_set; DELETE FROM method_version; DELETE FROM method;");
  }

  @Test
  void testConnection() {
    Assertions.assertTrue(postgres.isRunning());
    Assertions.assertEquals("test_user", postgres.getUsername());
    Assertions.assertEquals("test_password", postgres.getPassword());
    Assertions.assertEquals("test_db", postgres.getDatabaseName());
  }

  @Test
  void independenceTestA() {
    List<RunSet> runSets = runSetDao.getRunSets(null, true);
    Assertions.assertEquals(0, runSets.size());

    methodDao.createMethod(method);
    methodVersionDao.createMethodVersion(methodVersion);
    runSetDao.createRunSet(runSet);

    List<RunSet> runSetsAfter = runSetDao.getRunSets(null, true);
    Assertions.assertEquals(1, runSetsAfter.size());
  }

  @Test
  void independenceTestB() {
    List<RunSet> runSets = runSetDao.getRunSets(null, true);
    Assertions.assertEquals(0, runSets.size());
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
}
