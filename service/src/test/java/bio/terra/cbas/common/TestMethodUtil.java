package bio.terra.cbas.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.dockstore.client.ApiException;
import bio.terra.dockstore.model.ToolDescriptor;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestMethodUtil {
  DockstoreService dockstoreService = Mockito.mock(DockstoreService.class);

  @Test
  void conversionOfRawGithubUrl()
      throws MalformedURLException, UnsupportedEncodingException, URISyntaxException, ApiException {
    String rawGithubUrl =
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";
    String actualUrl =
        MethodUtil.convertToRawUrl(
            rawGithubUrl, PostMethodRequest.MethodSourceEnum.GITHUB, "master", dockstoreService);

    assertEquals(rawGithubUrl, actualUrl);
  }

  @Test
  void conversionOfGithubUrl()
      throws MalformedURLException, UnsupportedEncodingException, URISyntaxException, ApiException {
    String originalGithubUrl =
        "https://github.com/broadinstitute/viral-pipelines/blob/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";
    String expectedRawGithubUrl =
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";

    String actualUrl =
        MethodUtil.convertToRawUrl(
            originalGithubUrl,
            PostMethodRequest.MethodSourceEnum.GITHUB,
            "master",
            dockstoreService);

    assertEquals(expectedRawGithubUrl, actualUrl);
  }

  @Test
  void conversionOfInvalidGithubUrl() {
    String invalidUrl = "my-protocol://github.wrong-domain/repo/wdl-path.com";

    MalformedURLException exceptionThrown =
        assertThrows(
            MalformedURLException.class,
            () ->
                MethodUtil.convertToRawUrl(
                    invalidUrl,
                    PostMethodRequest.MethodSourceEnum.GITHUB,
                    "master",
                    dockstoreService));

    assertEquals("unknown protocol: my-protocol", exceptionThrown.getMessage());
  }

  @Test
  void conversionOfDockstoreUrl()
      throws MalformedURLException, UnsupportedEncodingException, URISyntaxException, ApiException {
    String workflowPath = "github.com/broadinstitute/viral-pipelines/fetch_sra_to_bam";
    String expectedRawGithubUrl =
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";

    ToolDescriptor toolDescriptor = new ToolDescriptor();
    toolDescriptor.setUrl(
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl");
    when(dockstoreService.descriptorGetV1(any(), any())).thenReturn(toolDescriptor);

    String actualUrl =
        MethodUtil.convertToRawUrl(
            workflowPath, PostMethodRequest.MethodSourceEnum.DOCKSTORE, "master", dockstoreService);

    verify(dockstoreService).descriptorGetV1(workflowPath, "master");
    assertEquals(expectedRawGithubUrl, actualUrl);
  }

  @Test
  void conversionOfInvalidDockstoreUrl()
      throws ApiException, MalformedURLException, UnsupportedEncodingException, URISyntaxException {
    String workflowPath = "github.com/wrong-repo/fetch_sra_to_bam";

    when(dockstoreService.descriptorGetV1(any(), any()))
        .thenThrow(new ApiException("Error thrown for testing purposes"));

    ApiException exceptionThrow =
        assertThrows(
            ApiException.class,
            () ->
                MethodUtil.convertToRawUrl(
                    workflowPath,
                    PostMethodRequest.MethodSourceEnum.DOCKSTORE,
                    "master",
                    dockstoreService));

    assertEquals("Error thrown for testing purposes", exceptionThrow.getMessage());
  }
}
