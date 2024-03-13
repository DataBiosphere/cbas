package bio.terra.cbas.dao;

import bio.terra.cbas.initialization.InstanceInitializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "spring.main.allow-bean-definition-overriding=true",
      "spring.liquibase.enabled=false"
    })
@ActiveProfiles("test")
@Testcontainers
public class TestRunSetDaoContainerized {
  @Autowired RunSetDao runSetDao;
  @MockBean private InstanceInitializer instanceInitializerMock;

  @Container @ServiceConnection
  static JdbcDatabaseContainer postgres =
      new PostgreSQLContainer("postgres:14")
          .withDatabaseName("test-db")
          .withUsername("test-user")
          .withPassword("test-password")
          .withInitScript("postgres-init.sql");

  @BeforeAll
  static void setup() {
    postgres.start();
  }

  @Test
  void testConnection() {
    Assertions.assertTrue(postgres.isRunning());
    Assertions.assertEquals("test-user", postgres.getUsername());
    Assertions.assertEquals("test-password", postgres.getPassword());
    Assertions.assertEquals("test-db", postgres.getDatabaseName());

    // List<RunSet> runSets = runSetDao.getRunSets(null, false);
    // Assertions.assertEquals(0, runSets.size());
  }
}
