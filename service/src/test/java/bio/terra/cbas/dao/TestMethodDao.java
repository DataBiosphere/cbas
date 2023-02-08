package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.models.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestMethodDao {

  @Autowired MethodDao methodDao;
  UUID methodId1 = UUID.randomUUID();
  UUID methodId2 = UUID.randomUUID();
  String methodName = "test method";
  String methodDesc = "test method description";
  String methodSource = "GitHub";

  Method method1 =
      new Method(
          methodId1, methodName, methodDesc, DateUtils.currentTimeInUTC(), null, methodSource);
  Method method2 =
      new Method(
          methodId2, methodName, methodDesc, DateUtils.currentTimeInUTC(), null, methodSource);

  @BeforeAll
  void setUp() {
    int recordsCreated1 = methodDao.createMethod(method1);
    int recordsCreated2 = methodDao.createMethod(method2);

    assertEquals(1, recordsCreated1);
    assertEquals(1, recordsCreated2);
  }

  @AfterAll
  void cleanUp() {
    int recordsDeleted1 = methodDao.deleteMethod(methodId1);
    int recordsDeleted2 = methodDao.deleteMethod(methodId2);

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

    assertEquals(5, allMethods.size()); // because database has 5 pre-staged workflows

    assertTrue(allMethods.stream().anyMatch(m -> m.methodId().equals(method1.methodId())));
    assertTrue(allMethods.stream().anyMatch(m -> m.methodId().equals(method2.methodId())));
  }
}
