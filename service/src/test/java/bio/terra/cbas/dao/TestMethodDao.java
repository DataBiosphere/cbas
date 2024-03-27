package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.dao.util.ContainerizedDatabaseTest;
import bio.terra.cbas.models.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class TestMethodDao extends ContainerizedDatabaseTest {

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

  @BeforeEach
  void init() {
    int recordsCreated1 = methodDao.createMethod(method1);
    int recordsCreated2 = methodDao.createMethod(method2);

    assertEquals(1, recordsCreated1);
    assertEquals(1, recordsCreated2);
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
  void unsetLastRunSetId() {
    methodDao.unsetLastRunSetId(method1.methodId());
    Method updatedMethod = methodDao.getMethod(method1.methodId());
    assertNull(updatedMethod.lastRunSetId());
  }
}
