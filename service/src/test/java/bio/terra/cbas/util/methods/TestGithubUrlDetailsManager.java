package bio.terra.cbas.util.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = {GithubUrlDetailsManager.class})
class TestGithubUrlDetailsManager {

  @Test
  void returnsExpectedUrlComponents() {
    String githubRawUrl =
        "raw.githubusercontent.com/broadinstitute/cromwell/develop/wdl/transforms/draft3/src/test/cases/simple_task.wdl";

    GithubUrlDetailsManager detailsManager = new GithubUrlDetailsManager();

    GithubUrlDetailsManager.GithubUrlComponents urlComponents =
        detailsManager.extractDetailsFromUrl(githubRawUrl);

    assertEquals("broadinstitute", urlComponents.getOrganization());

    assertEquals("cromwell", urlComponents.getRepository());

    assertEquals("develop", urlComponents.getBranchOrTag());

    assertEquals("wdl/transforms/draft3/src/test/cases/simple_task.wdl", urlComponents.getPath());
  }
}
