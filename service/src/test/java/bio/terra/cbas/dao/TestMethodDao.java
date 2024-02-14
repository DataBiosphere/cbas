package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.models.GithubMethodSource;
import bio.terra.cbas.models.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestMethodDao {

  @MockBean MicrometerMetrics micrometerMetrics;
  @Autowired MethodDao methodDao;
  UUID methodId1 = UUID.randomUUID();
  UUID methodId2 = UUID.randomUUID();
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
  Method method2 =
      new Method(
          methodId2,
          "test method 2",
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodSource,
          workspaceId);

  GithubMethodSource details =
      new GithubMethodSource("cromwell", "broadinstitute", "cbas/dao/test.py", true, methodId1);

  @BeforeAll
  void setUp() {
    int recordsCreated1 = methodDao.createMethod(method1);
    int recordsCreated2 = methodDao.createMethod(method2);
    int githubMethodDetails = methodDao.createGithubMethodSourceDetails(details);

    assertEquals(1, recordsCreated1);
    assertEquals(1, recordsCreated2);
    assertEquals(1, githubMethodDetails);
  }

  @AfterAll
  void cleanUp() {
    int detailsDeleted = methodDao.deleteMethodSourceDetails(methodId1);
    int recordsDeleted1 = methodDao.deleteMethod(methodId1);
    int recordsDeleted2 = methodDao.deleteMethod(methodId2);

    assertEquals(1, detailsDeleted);
    assertEquals(1, recordsDeleted1);
    assertEquals(1, recordsDeleted2);
  }

  @Test
  void retrievesSingleMethod() {
    Method actual = methodDao.getMethod(methodId1);

    /*
    Asserting each column value separately here and omitting the 'created' column due to github
    passing in a current_timestamp() value, causing the test to fail.
    */

    assertEquals(methodId1, actual.methodId());
    assertEquals(methodName, actual.name());
    assertEquals(methodDesc, actual.description());
    assertEquals(methodSource, actual.methodSource());
    assertNull(actual.lastRunSetId());
  }

  @Test
  void retrievesAllMethods() {
    List<Method> allMethods = methodDao.getMethods();
    assertEquals(2, allMethods.size());

    assertTrue(allMethods.stream().anyMatch(m -> m.methodId().equals(method1.methodId())));
    assertTrue(allMethods.stream().anyMatch(m -> m.methodId().equals(method2.methodId())));

    // ensure that methods are listed in desc order of creation
    assertEquals(method2.methodId(), allMethods.get(0).methodId());
    assertEquals(method1.methodId(), allMethods.get(1).methodId());
  }

  @Test
  void retrievesGithubMethodDetails() {
    GithubMethodSource methodDetails = methodDao.getMethodSourceDetails(methodId1);
    assertEquals(methodId1, methodDetails.methodId());
    assertEquals("cromwell", methodDetails.repository());
    assertEquals("cbas/dao/test.py", methodDetails.path());
    assertEquals("broadinstitute", methodDetails.organization());
    assertEquals(true, methodDetails._private());
  }
}
