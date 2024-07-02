package bio.terra.cbas.service;

import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.DOCKSTORE;
import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.GITHUB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.model.MethodVersionDetails;
import bio.terra.cbas.models.CbasMethodStatus;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.dockstore.client.ApiException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestMethodVersionService {

  Method getSubmissionUrlBaseMethod =
      new Method(
          UUID.randomUUID(),
          "methodName",
          "methodDescription",
          OffsetDateTime.now(),
          UUID.randomUUID(),
          GITHUB.toString(),
          UUID.randomUUID(),
          Optional.empty(),
          CbasMethodStatus.ACTIVE);

  MethodVersion getSubmissionUrlBaseMethodVersion =
      new MethodVersion(
          UUID.randomUUID(),
          getSubmissionUrlBaseMethod,
          "version name",
          "version description",
          OffsetDateTime.now(),
          UUID.randomUUID(),
          "https://github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
          UUID.randomUUID(),
          "develop",
          Optional.empty());

  Method getSubmissionUrlMethodWithGithubDetails =
      getSubmissionUrlBaseMethod.withGithubMethodDetails(
          new GithubMethodDetails(
              "cromwell",
              "broadinstitute",
              "centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl",
              false,
              getSubmissionUrlBaseMethod.methodId()));

  MethodVersion getSubmissionUrlBaseMethodVersionWithGithubMethodDetails =
      getSubmissionUrlBaseMethodVersion.withMethod(getSubmissionUrlMethodWithGithubDetails);

  MethodVersion getSubmissionUrlBaseMethodVersionWithGithubMethodAndMethodVersionDetails =
      getSubmissionUrlBaseMethodVersionWithGithubMethodDetails.withMethodVersionDetails(
          new GithubMethodVersionDetails(
              "abcd123",
              getSubmissionUrlBaseMethodVersionWithGithubMethodDetails.methodVersionId()));

  @Test
  void getSubmissionUrl_githubWithoutMethodDetails() throws Exception {
    // Even though we have the plain github.com address in the URL, we expect the raw URL for
    // submitting:
    String expected =
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl";
    String actual = MethodVersionService.getSubmissionUrl(getSubmissionUrlBaseMethodVersion, null);
    assertEquals(expected, actual);
  }

  @Test
  void getSubmissionUrl_rawGithubUrlWithoutMethodDetails() throws Exception {
    MethodVersion withRawGithubUrl =
        getSubmissionUrlBaseMethodVersion.withUrl(
            "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl");
    // With the raw URL provided, we still expect the URL as a result:
    String expected =
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl";
    String actual = MethodVersionService.getSubmissionUrl(withRawGithubUrl, null);
    assertEquals(expected, actual);
  }

  @Test
  void getSubmissionUrl_githubWithMethodDetailsOnly() throws Exception {
    String expected =
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl";
    String actual =
        MethodVersionService.getSubmissionUrl(
            getSubmissionUrlBaseMethodVersionWithGithubMethodDetails, null);
    assertEquals(expected, actual);
  }

  @Test
  void getSubmissionUrl_githubWithMethodDetailsAndMethodVersionDetails() throws Exception {
    // The presence of the method version details allows us to construct a commit-specific path:
    String expected =
        "https://raw.githubusercontent.com/broadinstitute/cromwell/abcd123/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl";
    String actual =
        MethodVersionService.getSubmissionUrl(
            getSubmissionUrlBaseMethodVersionWithGithubMethodAndMethodVersionDetails, null);
    assertEquals(expected, actual);
  }

  @Test
  void getSubmissionUrl_githubWithMethodVersionDetailsOnly() throws Exception {
    MethodVersion withMethodVersionDetailsOnly =
        getSubmissionUrlBaseMethodVersionWithGithubMethodAndMethodVersionDetails.withMethod(
            getSubmissionUrlBaseMethod);
    // This case (method version details but no method details should never come up in production).
    // Since this doesn't count as "everything there", we expect it to fall back to using the URL in
    // the DB.
    String expected =
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/forkjoin/forkjoin.wdl";
    String actual = MethodVersionService.getSubmissionUrl(withMethodVersionDetailsOnly, null);
    assertEquals(expected, actual);
  }

  Method getSubmissionUrlDockstoreBaseMethod =
      new Method(
          UUID.randomUUID(),
          "HelloWorld",
          null,
          OffsetDateTime.now(),
          null,
          DOCKSTORE.toString(),
          UUID.randomUUID(),
          Optional.empty(),
          CbasMethodStatus.ACTIVE);

  MethodVersion getSubmissionUrlDockstoreBaseMethodVersion =
      new MethodVersion(
          UUID.randomUUID(),
          getSubmissionUrlDockstoreBaseMethod,
          "develop",
          null,
          OffsetDateTime.now(),
          null,
          "github.com/dockstore/bcc2020-training/HelloWorld",
          getSubmissionUrlDockstoreBaseMethod.originalWorkspaceId(),
          "develop",
          Optional.empty());

  @Test
  void getSubmissionUrl_dockstoreMethod() throws Exception {
    MethodVersion versionUnderTest = getSubmissionUrlDockstoreBaseMethodVersion;
    String expected =
        "https://raw.githubusercontent.com/dockstore/bcc2020-training/master/wdl-training/exercise1/HelloWorld.wdl";

    DockstoreService mockstoreService = mock(DockstoreService.class);

    when(mockstoreService.resolveDockstoreUrl(versionUnderTest))
        .thenReturn(
            "https://raw.githubusercontent.com/dockstore/bcc2020-training/master/wdl-training/exercise1/HelloWorld.wdl");

    String actual = MethodVersionService.getSubmissionUrl(versionUnderTest, mockstoreService);
    assertEquals(expected, actual);
  }

  @Test
  void testDockstoreMethodVersionToMethodVersionDetails()
      throws MethodProcessingException, ApiException {

    String resolvedUrl = "http://my-resolved-wdl.dockstore.org";

    DockstoreService dockstoreService = mock(DockstoreService.class);
    when(dockstoreService.resolveDockstoreUrl(any())).thenReturn(resolvedUrl);
    MethodVersionService methodVersionService = new MethodVersionService(dockstoreService);

    Method method =
        new Method(null, null, null, null, null, DOCKSTORE.toString(), null, null, null);

    MethodVersion methodVersion =
        new MethodVersion(null, method, null, null, null, null, null, null, null, null);

    MethodVersionDetails details =
        methodVersionService.methodVersionToMethodVersionDetails(methodVersion);
    assertEquals(resolvedUrl, details.getUrl());
  }

  @Test
  void testGithubMethodVersionToMethodVersionDetails() {

    String inputUrl = "http://my-input-wdl.github.com";
    MethodVersionService methodVersionService = new MethodVersionService(null);

    GithubMethodDetails methodDetails =
        new GithubMethodDetails("myRepo", "myOrg", "myPath", false, UUID.randomUUID());

    GithubMethodVersionDetails methodVersionDetails =
        new GithubMethodVersionDetails("myGitHash", UUID.randomUUID());

    Method method =
        new Method(
            null,
            null,
            null,
            null,
            null,
            GITHUB.toString(),
            null,
            Optional.of(methodDetails),
            null);
    MethodVersion methodVersion =
        new MethodVersion(
            UUID.randomUUID(),
            method,
            null,
            null,
            null,
            null,
            inputUrl,
            null,
            null,
            Optional.of(methodVersionDetails));

    MethodVersionDetails details =
        methodVersionService.methodVersionToMethodVersionDetails(methodVersion);

    assertEquals(
        "https://raw.githubusercontent.com/myOrg/myRepo/myGitHash/myPath", details.getUrl());
  }

  @Test
  void testOtherMethodVersionToMethodVersionDetails() {
    String resolvedUrl = "http://my-resolved-wdl.someotherdomain.org";
    MethodVersionService methodVersionService = new MethodVersionService(null);

    Method method = new Method(null, null, null, null, null, "someOtherDomain", null, null, null);

    MethodVersion methodVersion =
        new MethodVersion(null, method, null, null, null, null, resolvedUrl, null, null, null);

    MethodVersionDetails details =
        methodVersionService.methodVersionToMethodVersionDetails(methodVersion);
    assertEquals(resolvedUrl, details.getUrl());
  }
}
