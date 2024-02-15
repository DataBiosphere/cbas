package bio.terra.cbas.util.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = {GithubUrlDetailsManager.class})
class TestGithubUrlDetailsManager {

  @Test
  void returnsExpectedUrlComponents() throws URISyntaxException {
    String githubRawUrl =
        "raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl";
    String httpsGithubRawUrl =
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl";

    GithubUrlDetailsManager detailsManager = new GithubUrlDetailsManager();

    GithubUrlDetailsManager.GithubUrlComponents urlComponents =
        detailsManager.extractDetailsFromUrl(githubRawUrl);
    GithubUrlDetailsManager.GithubUrlComponents urlComponents2 =
        detailsManager.extractDetailsFromUrl(httpsGithubRawUrl);

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
