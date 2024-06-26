package bio.terra.cbas.util.methods;

import static bio.terra.cbas.common.MethodUtil.extractGithubUrlComponents;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.exceptions.MethodProcessingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class TestGithubUrlComponents {
  record TestCase(String org, String repo, String branchOrTag, String path, String url) {

    public void check()
        throws URISyntaxException, MalformedURLException, MethodProcessingException {
      assertEquals(extractGithubUrlComponents(url).org(), org);
      assertEquals(extractGithubUrlComponents(url).path(), path);
      assertEquals(extractGithubUrlComponents(url).branchOrTag(), branchOrTag);
      assertEquals(extractGithubUrlComponents(url).repo(), repo);
    }

    public void UriExceptionCheck() {
      assertThrows(URISyntaxException.class, () -> extractGithubUrlComponents(url));
    }
  }

  @TestFactory
  Stream<DynamicTest> returnExpectedUrlComponents() {

    var testCases =
        new TestCase[] {
          new TestCase(
              "broadinstitute",
              "cromwell",
              "develop",
              "wdl/transforms/draft3/src/test/cases/simple_task.wdl",
              "raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl"),
          new TestCase(
              "broadinstitute",
              "cromwell",
              "develop",
              "wdl/transforms/draft3/src/test/cases/simple_task.wdl",
              "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl"),
        };

    return DynamicTest.stream(Stream.of(testCases), TestCase::url, TestCase::check);
  }

  @TestFactory
  Stream<DynamicTest> throwsUriException() {
    var testCases =
        new TestCase[] {
          new TestCase(null, null, null, null, "not a url"),
        };
    return DynamicTest.stream(Stream.of(testCases), TestCase::url, TestCase::UriExceptionCheck);
  }
}
