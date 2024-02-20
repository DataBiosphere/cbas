package bio.terra.cbas.util.methods;

import static bio.terra.cbas.common.MethodUtil.extractGithubDetailsFromUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import bio.terra.cbas.util.methods.GithubUrlDetailsManager.GithubUrlComponents;

import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {GithubUrlDetailsManager.class})
class TestGithubUrlDetailsManager {

  @Test
  void returnsExpectedUrlComponents() throws URISyntaxException {
    String githubRawUrl =
        "raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl";
    String httpsGithubRawUrl =
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl";

    GithubUrlComponents urlComponents = extractGithubDetailsFromUrl(githubRawUrl);
    GithubUrlComponents urlComponents2 = extractGithubDetailsFromUrl(httpsGithubRawUrl);

    assertEquals("broadinstitute", urlComponents.getOrganization());
    assertEquals("broadinstitute", urlComponents2.getOrganization());

    assertEquals("cromwell", urlComponents.getRepository());
    assertEquals("cromwell", urlComponents2.getRepository());

    assertEquals("develop", urlComponents.getBranchOrTag());
    assertEquals("develop", urlComponents2.getBranchOrTag());

    assertEquals("wdl/transforms/draft3/src/test/cases/simple_task.wdl", urlComponents.getPath());
    assertEquals("wdl/transforms/draft3/src/test/cases/simple_task.wdl", urlComponents2.getPath());
  }
}
