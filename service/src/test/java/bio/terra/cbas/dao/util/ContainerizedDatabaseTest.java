package bio.terra.cbas.dao.util;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
// Without @DirtiesContext, multiple ContainerizedDatabaseTest classes executed in rapid succession
// can cause loss of connection to the containerized database.
// See: https://stackoverflow.com/a/68992727
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
public abstract class ContainerizedDatabaseTest {

  public static final String TEST_DB_NAME = "test_db";
  public static final String TEST_USER = "test_user";
  public static final String TEST_PASSWORD = "test_password";
  public static final String TEST_ROLE = "test_role";

  @Container
  protected static final JdbcDatabaseContainer postgres =
      new PostgreSQLContainer("postgres:14")
          .withDatabaseName(TEST_DB_NAME)
          .withUsername(TEST_USER)
          .withPassword(TEST_PASSWORD);

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.jdbc-url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.liquibase.parameters.dbRole", () -> TEST_ROLE);
    registry.add("cbas.cbas-database.uri", postgres::getJdbcUrl);
    registry.add("cbas.cbas-database.username", postgres::getUsername);
    registry.add("cbas.cbas-database.password", postgres::getPassword);
  }

  @BeforeAll
  public static void setup() throws SQLException {
    postgres.start();
    DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .createStatement()
        .execute(
            "CREATE ROLE %s;".formatted(TEST_ROLE)
                + "GRANT %s TO %s;".formatted(TEST_ROLE, TEST_USER));
  }

  @AfterEach
  public void cleanupDb() throws SQLException {
    DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .createStatement()
        .execute(
            "TRUNCATE TABLE run CASCADE; "
                + "TRUNCATE TABLE method_version CASCADE; "
                + "TRUNCATE TABLE run_set CASCADE; "
                + "TRUNCATE TABLE method CASCADE; "
                + "TRUNCATE TABLE github_method_details CASCADE; "
                + "TRUNCATE TABLE github_method_version_details CASCADE; ");
  }
}
