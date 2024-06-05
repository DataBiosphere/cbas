package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.common.exceptions.MethodNotFoundException;
import bio.terra.cbas.dao.util.ContainerizedDatabaseTest;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;

class TestMethodDao extends ContainerizedDatabaseTest {

  @MockBean MicrometerMetrics micrometerMetrics;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;
  @Autowired RunSetDao runSetDao;
  UUID methodId1 = UUID.randomUUID();
  UUID methodId2 = UUID.randomUUID();
  UUID methodWithGithubDetailsId = UUID.randomUUID();
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
          workspaceId,
          Optional.empty(),
          false);

  Method method2 =
      new Method(
          methodId2,
          "test method 2",
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodSource,
          workspaceId,
          Optional.empty(),
          false);

  Method methodWithGithubDetails =
      new Method(
          methodWithGithubDetailsId,
          "test method with github details",
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodSource,
          workspaceId,
          Optional.of(
              new GithubMethodDetails("repo", "org", "path", false, methodWithGithubDetailsId)),
          false);

  MethodVersion methodVersion =
      new MethodVersion(
          UUID.fromString("80000000-0000-0000-0000-000000000008"),
          method1,
          "1.0",
          "fetch_sra_to_bam sample submission",
          OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
          null,
          "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl",
          workspaceId,
          "develop",
          Optional.empty());
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
    methodVersionDao.createMethodVersion(methodVersion);
    runSetDao.createRunSet(runSet);
    methodDao.updateLastRunWithRunSet(runSet);
    Method retrievedMethod = methodDao.getMethod(methodId1);
    assertEquals(runSet.runSetId(), retrievedMethod.lastRunSetId());
    methodDao.unsetLastRunSetId(retrievedMethod.methodId());
    Method updatedMethod = methodDao.getMethod(retrievedMethod.methodId());
    assertNull(updatedMethod.lastRunSetId());
  }

  @Test
  void createsMethodWithGithubDetails() {
    // Create:
    int recordsCreated = methodDao.createMethod(methodWithGithubDetails);
    assertEquals(1, recordsCreated);

    // Check fields:
    Method actual = methodDao.getMethod(methodWithGithubDetailsId);
    assertEquals(methodWithGithubDetailsId, actual.methodId());
    assertEquals("test method with github details", actual.name());
    assertEquals("test method description", actual.description());
    assertEquals("GitHub", actual.methodSource());
    assertEquals("repo", actual.githubMethodDetails().get().repository());
    assertEquals("org", actual.githubMethodDetails().get().organization());
    assertEquals("path", actual.githubMethodDetails().get().path());
    assertEquals(false, actual.githubMethodDetails().get().isPrivate());
  }

  @Test
  void archiveMethod() {
    List<Method> allMethods = methodDao.getMethods();
    assertEquals(2, allMethods.size());

    methodDao.archiveMethod(method1.methodId());
    List<Method> remainingMethods = methodDao.getMethods();
    assertEquals(1, remainingMethods.size());
  }

  @Test
  void getArchivedMethodFails() {
    int recordArchived = methodDao.archiveMethod(method1.methodId());
    assertEquals(1, recordArchived);
    assertThrows(MethodNotFoundException.class, () -> methodDao.getMethod(method1.methodId()));
  }

  @Test
  void recreateArchivedMethodWithIdenticalIdFails() {
    int recordArchived = methodDao.archiveMethod(method1.methodId());
    assertEquals(1, recordArchived);

    assertThrows(DuplicateKeyException.class, () -> methodDao.createMethod(method1));
  }

  @Test
  void recreateArchivedMethodWithIdenticalNameSucceeds() {

    int recordArchived = methodDao.archiveMethod(method1.methodId());
    assertEquals(1, recordArchived);

    Method methodWithIdenticalName =
        new Method(
            UUID.randomUUID(),
            methodName,
            methodDesc,
            DateUtils.currentTimeInUTC(),
            null,
            methodSource,
            workspaceId,
            Optional.empty(),
            false);

    int methodCreationSucceeds = methodDao.createMethod(methodWithIdenticalName);
    assertEquals(1, methodCreationSucceeds);
  }
}
