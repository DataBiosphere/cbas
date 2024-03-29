package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.util.ContainerizedDatabaseTest;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TestMethodVersionDao extends ContainerizedDatabaseTest {

  @Autowired MethodVersionDao methodVersionDao;
  @Autowired MethodDao methodDao;
  @Autowired RunSetDao runSetDao;

  UUID methodId = UUID.randomUUID();
  UUID methodVersionId = UUID.randomUUID();
  String methodName = "test method";
  String methodDesc = "test method description";
  String methodSource = "GitHub";
  String methodVersionName = "develop";
  String methodUrl =
      "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl";

  UUID workspaceId = UUID.randomUUID();
  String branch = "develop";

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
          workspaceId,
          branch);

  RunSet runSet =
      new RunSet(
          UUID.randomUUID(),
          methodVersion,
          "fetch_sra_to_bam workflow",
          "fetch_sra_to_bam sample submission",
          false,
          false,
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

  @BeforeEach
  void init() {
    int methodRecordsCreated = methodDao.createMethod(method);
    int methodVersionRecordsCreated = methodVersionDao.createMethodVersion(methodVersion);

    assertEquals(1, methodRecordsCreated);
    assertEquals(1, methodVersionRecordsCreated);
  }

  @Test
  void retrievesSingleMethodVersion() {
    MethodVersion actual = methodVersionDao.getMethodVersion(methodVersionId);

    assertEquals(methodVersionId, actual.methodVersionId());
    assertEquals(methodVersionName, actual.name());
    assertEquals(methodDesc, actual.description());
    assertEquals(methodUrl, actual.url());
    assertEquals(branch, actual.branchOrTagName());
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
    assertEquals(branch, actual.get(0).branchOrTagName());
    assertNull(actual.get(0).lastRunSetId());
  }

  @Test
  void unsetLastRunSetId() {
    runSetDao.createRunSet(runSet);
    methodVersionDao.updateLastRunWithRunSet(runSet);
    MethodVersion retrievedMethodVersion =
        methodVersionDao.getMethodVersion(methodVersion.methodVersionId());
    assertEquals(runSet.runSetId(), retrievedMethodVersion.lastRunSetId());
    methodVersionDao.unsetLastRunSetId(methodVersion.methodVersionId());
    MethodVersion updatedMethodVersion =
        methodVersionDao.getMethodVersion(methodVersion.methodVersionId());
    assertNull(updatedMethodVersion.lastRunSetId());
  }
}
