package bio.terra.cbas.util.methods;

import static bio.terra.cbas.common.MethodUtil.extractGithubDetailsFromUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class TestGithubUrlComponents {

  @Test
  void returnsExpectedUrlComponents() throws URISyntaxException {

    List<Map<String, String>> githubMethods = new ArrayList<>();
    Map<String, String> source1 = new HashMap<>();
    Map<String, String> source2 = new HashMap<>();

    source1.put("org", "broadinstitute");
    source1.put("repo", "cromwell");
    source1.put("branchOrTag", "develop");
    source1.put("path", "wdl/transforms/draft3/src/test/cases/simple_task.wdl");
    source1.put(
        "url",
        "raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl");

    source2.put("org", "broadinstitute");
    source2.put("repo", "cromwell");
    source2.put("branchOrTag", "develop");
    source2.put("path", "wdl/transforms/draft3/src/test/cases/simple_task.wdl");
    source2.put(
        "url",
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl");

    githubMethods.add(source1);
    githubMethods.add(source2);

    for (Map<String, String> method : githubMethods) {
      GithubUrlComponents urlComponents = extractGithubDetailsFromUrl(method.get("url"));
      assertEquals(method.get("org"), urlComponents.org());
      assertEquals(method.get("repo"), urlComponents.repo());
      assertEquals(method.get("branchOrTag"), urlComponents.branchOrTag());
      assertEquals(method.get("path"), urlComponents.path());
    }

    githubMethods.clear();
  }

  @TestFactory
  Stream<DynamicTest> tableDrivenTest() {

    record TestCase(String org, String repo, String branchOrTag, String path, String url) {

      public void check() throws URISyntaxException {
        assertEquals(extractGithubDetailsFromUrl(url).org(), org);
        assertEquals(extractGithubDetailsFromUrl(url).path(), path);
        assertEquals(extractGithubDetailsFromUrl(url).branchOrTag(), branchOrTag);
        assertEquals(extractGithubDetailsFromUrl(url).repo(), repo);
      }
    }

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
}
