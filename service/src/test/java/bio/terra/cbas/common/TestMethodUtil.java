package bio.terra.cbas.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.model.PostMethodRequest;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestMethodUtil {
  @Test
  void conversionOfRawGithubUrl() throws MalformedURLException, URISyntaxException {
    String rawGithubUrl =
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";
    String actualUrl = MethodUtil.asRawMethodUrlGithub(rawGithubUrl);

    assertEquals(rawGithubUrl, actualUrl);
  }

  @Test
  void conversionOfGithubUrl() throws MalformedURLException, URISyntaxException {
    String originalGithubUrl =
        "https://github.com/broadinstitute/viral-pipelines/blob/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";
    String expectedRawGithubUrl =
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";

    String actualUrl = MethodUtil.asRawMethodUrlGithub(originalGithubUrl);

    assertEquals(expectedRawGithubUrl, actualUrl);
  }

  @Test
  void conversionOfInvalidGithubUrl() {
    String invalidUrl = "my-protocol://github.wrong-domain/repo/wdl-path.com";

    MalformedURLException exceptionThrown =
        assertThrows(
            MalformedURLException.class, () -> MethodUtil.asRawMethodUrlGithub(invalidUrl));

    assertEquals("unknown protocol: my-protocol", exceptionThrown.getMessage());
  }

  @Test
  void conversionOfGithubToMethodSourceEnum()
      throws MethodProcessingException.UnknownMethodSourceException {
    assertEquals(
        PostMethodRequest.MethodSourceEnum.GITHUB, MethodUtil.convertToMethodSourceEnum("Github"));
  }

  @Test
  void conversionOfGitHubToMethodSourceEnum()
      throws MethodProcessingException.UnknownMethodSourceException {
    assertEquals(
        PostMethodRequest.MethodSourceEnum.GITHUB, MethodUtil.convertToMethodSourceEnum("GitHub"));
  }

  @Test
  void conversionOfDockstoreToMethodSourceEnum()
      throws MethodProcessingException.UnknownMethodSourceException {
    assertEquals(
        PostMethodRequest.MethodSourceEnum.DOCKSTORE,
        MethodUtil.convertToMethodSourceEnum("Dockstore"));
  }

  @Test
  void conversionOfUnknownMethodSource() {
    MethodProcessingException.UnknownMethodSourceException unknownSourceException =
        assertThrows(
            MethodProcessingException.UnknownMethodSourceException.class,
            () -> MethodUtil.convertToMethodSourceEnum("MyMethodSource"));
    assertEquals("Unknown method source: MyMethodSource", unknownSourceException.getMessage());
  }
}
