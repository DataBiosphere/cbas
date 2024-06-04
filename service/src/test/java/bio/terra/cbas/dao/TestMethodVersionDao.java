package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.util.ContainerizedDatabaseTest;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
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

  UUID workspaceId = UUID.randomUUID();
  String branch = "develop";

  String gitHubRepository = "cromwell";
  String gitHubOrganization = "broadinstitute";
  String gitHubPath = "centaur/src/main/resources/standardTestCases/hello/hello.wdl";

  String rawMethodUrl =
      "https://raw.githubusercontent.com/%s/%s/%s/%s"
          .formatted(gitHubOrganization, gitHubRepository, branch, gitHubPath);

  GithubMethodDetails githubMethodDetails =
      new GithubMethodDetails(gitHubRepository, gitHubOrganization, gitHubPath, false, methodId);
  Method method =
      new Method(
          methodId,
          methodName,
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodSource,
          workspaceId,
          Optional.empty());
  MethodVersion methodVersion =
      new MethodVersion(
          methodVersionId,
          method,
          methodVersionName,
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          rawMethodUrl,
          workspaceId,
          branch,
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

  void createMethodAndVersion(Method method, MethodVersion methodVersion) {
    int methodRecordsCreated = methodDao.createMethod(method);
    assertEquals(1, methodRecordsCreated);
    int methodVersionRecordsCreated = methodVersionDao.createMethodVersion(methodVersion);
    assertEquals(1, methodVersionRecordsCreated);
  }

  @Test
  void retrievesSingleMethodVersion() {
    createMethodAndVersion(method, methodVersion);

    MethodVersion actual = methodVersionDao.getMethodVersion(methodVersionId);

    assertEquals(methodVersionId, actual.methodVersionId());
    assertEquals(methodVersionName, actual.name());
    assertEquals(methodDesc, actual.description());
    assertEquals(rawMethodUrl, actual.url());
    assertEquals(branch, actual.branchOrTagName());
    assertNull(actual.lastRunSetId());
    assertEquals(actual.methodVersionDetails(), Optional.empty());
  }

  @Test
  void retrievesMethodVersionsForMethod() {
    createMethodAndVersion(method, methodVersion);

    List<MethodVersion> actual = methodVersionDao.getMethodVersionsForMethod(method);

    assertEquals(1, actual.size());
    assertEquals(methodVersionId, actual.get(0).methodVersionId());
    assertEquals(methodId, actual.get(0).method().methodId());
    assertEquals(methodVersionName, actual.get(0).name());
    assertEquals(methodDesc, actual.get(0).description());
    assertEquals(rawMethodUrl, actual.get(0).url());
    assertEquals(branch, actual.get(0).branchOrTagName());
    assertNull(actual.get(0).lastRunSetId());
    assertEquals(actual.get(0).methodVersionDetails(), Optional.empty());
  }

  @Test
  void storeAndRetrieveGithashDuringLookupById() {
    String githash = "1234567890abcdef";
    GithubMethodVersionDetails githubMethodVersionDetails =
        new GithubMethodVersionDetails(githash, methodVersionId);
    MethodVersion methodVersionWithGithash =
        methodVersion.withMethodVersionDetails(githubMethodVersionDetails);

    createMethodAndVersion(method, methodVersionWithGithash);

    MethodVersion actual = methodVersionDao.getMethodVersion(methodVersionId);
    assertEquals(githash, actual.methodVersionDetails().get().githash());
  }

  private MethodVersion randomizedMethodVersion() {
    UUID methodVersionId = UUID.randomUUID();
    // Githash is a random 40-character hex string:
    String githash = RandomStringUtils.random(40, "0123456789abcdef");
    GithubMethodVersionDetails githubMethodVersionDetails =
        new GithubMethodVersionDetails(githash, methodVersionId);
    return methodVersion
        .withMethodVersionId(methodVersionId)
        .withMethodVersionDetails(githubMethodVersionDetails);
  }

  @Test
  void storeAndRetrieveGithashDuringLookupByList() {

    int methodRecordsCreated = methodDao.createMethod(method);
    assertEquals(1, methodRecordsCreated);

    List<MethodVersion> methodVersions =
        List.of(
            randomizedMethodVersion(),
            randomizedMethodVersion(),
            randomizedMethodVersion(),
            randomizedMethodVersion(),
            randomizedMethodVersion());

    for (MethodVersion methodVersion : methodVersions) {
      int methodVersionRecordsCreated = methodVersionDao.createMethodVersion(methodVersion);
      assertEquals(1, methodVersionRecordsCreated);
    }

    List<MethodVersion> actual = methodVersionDao.getMethodVersions();

    assertEquals(methodVersions.size(), actual.size());

    for (MethodVersion expectedVersion : methodVersions) {
      MethodVersion actualVersion =
          actual.stream()
              .filter(v -> v.methodVersionId().equals(expectedVersion.methodVersionId()))
              .findFirst()
              .orElseThrow();
      assertEquals(
          expectedVersion.methodVersionDetails().get().githash(),
          actualVersion.methodVersionDetails().get().githash());
    }
  }

  @Test
  void unsetLastRunSetId() {
    createMethodAndVersion(method, methodVersion);

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
