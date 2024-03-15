package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.Method;
import java.sql.DriverManager;
import java.sql.SQLException;
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
class TestGithubMethodDetailsDao {
  @Autowired GithubMethodDetailsDao githubMethodDetailsDao;

  @Autowired MethodDao methodDao;

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

  UUID methodId1 = UUID.randomUUID();
  String methodName = "test method";
  String methodDesc = "test method description";
  String methodSource = "GitHub";

  UUID workspaceId = UUID.randomUUID();

  Method method1 =
      new Method(
          methodId1,
          methodName,
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodSource,
          workspaceId);
  GithubMethodDetails details =
      new GithubMethodDetails("cromwell", "broadinstitute", "cbas/dao/test.py", true, methodId1);

  @BeforeAll
  static void setup() {
    postgres.start();
  }

  @BeforeEach
  void init() {
    int recordsCreated1 = methodDao.createMethod(method1);
    int githubMethodDetails = githubMethodDetailsDao.createGithubMethodSourceDetails(details);

    assertEquals(1, recordsCreated1);
    assertEquals(1, githubMethodDetails);
  }

  @AfterEach
  void cleanupDb() throws SQLException {
    DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .createStatement()
        .execute("DELETE FROM run_set; DELETE FROM method_version; DELETE FROM method;");
  }

  @Test
  void retrievesGithubMethodDetails() {
    GithubMethodDetails methodDetails = githubMethodDetailsDao.getMethodSourceDetails(methodId1);
    assertEquals(methodId1, methodDetails.methodId());
    assertEquals("cromwell", methodDetails.repository());
    assertEquals("cbas/dao/test.py", methodDetails.path());
    assertEquals("broadinstitute", methodDetails.organization());
    assertEquals(true, methodDetails.isPrivate());
  }
}
