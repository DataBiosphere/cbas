package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
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
class TestMethodVersionDao {

  @Autowired MethodVersionDao methodVersionDao;
  @Autowired MethodDao methodDao;

  UUID methodId = UUID.randomUUID();
  UUID methodVersionId = UUID.randomUUID();
  String methodName = "test method";
  String methodDesc = "test method description";
  String methodSource = "GitHub";
  String methodVersionName = "develop";
  String methodUrl =
      "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl";

  String workspaceId = UUID.randomUUID().toString();

  Method method =
      new Method(
          methodId,
          methodName,
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodSource,
          workspaceId);
  MethodVersion methodVersion =
      new MethodVersion(
          methodVersionId,
          method,
          methodVersionName,
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodUrl,
          workspaceId);

  @BeforeAll
  void setUp() {
    int methodRecordsCreated = methodDao.createMethod(method);
    int methodVersionRecordsCreated = methodVersionDao.createMethodVersion(methodVersion);

    assertEquals(1, methodRecordsCreated);
    assertEquals(1, methodVersionRecordsCreated);
  }

  @AfterAll
  void cleanUp() {
    int methodVersionRecordsDeleted = methodVersionDao.deleteMethodVersion(methodVersionId);
    int methodRecordsDeleted = methodDao.deleteMethod(methodId);

    assertEquals(1, methodVersionRecordsDeleted);
    assertEquals(1, methodRecordsDeleted);
  }

  @Test
  void retrievesSingleMethodVersion() {
    MethodVersion actual = methodVersionDao.getMethodVersion(methodVersionId);

    assertEquals(methodVersionId, actual.methodVersionId());
    assertEquals(methodVersionName, actual.name());
    assertEquals(methodDesc, actual.description());
    assertEquals(methodUrl, actual.url());
    assertNull(actual.lastRunSetId());
  }

  @Test
  void retrievesMethodVersionsForMethod() {
    List<MethodVersion> actual = methodVersionDao.getMethodVersionsForMethod(method);

    assertEquals(1, actual.size());
    assertEquals(methodVersionId, actual.get(0).methodVersionId());
    assertEquals(methodId, actual.get(0).method().methodId());
    assertEquals(methodVersionName, actual.get(0).name());
    assertEquals(methodDesc, actual.get(0).description());
    assertEquals(methodUrl, actual.get(0).url());
    assertNull(actual.get(0).lastRunSetId());
  }
}
