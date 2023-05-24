package bio.terra.cbas.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cbas.controllers.MethodsApiController;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.dockstore.client.ApiException;
import bio.terra.dockstore.model.ToolDescriptor;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = MethodsApiController.class)
public class TestMethodUtil {
  @MockBean private CromwellService cromwellService;
  @MockBean private DockstoreService dockstoreService;
  @MockBean private MethodDao methodDao;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private RunSetDao runSetDao;

  @Test
  void conversionOfRawGithubUrl()
      throws MalformedURLException, UnsupportedEncodingException, URISyntaxException, ApiException {
    String rawGithubUrl =
        "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl";
    String actualUrl =
        MethodUtil.convertToRawGithubUrl(
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
        MethodUtil.convertToRawGithubUrl(
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
                MethodUtil.convertToRawGithubUrl(
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
        MethodUtil.convertToRawGithubUrl(
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
                MethodUtil.convertToRawGithubUrl(
                    workflowPath,
                    PostMethodRequest.MethodSourceEnum.DOCKSTORE,
                    "master",
                    dockstoreService));

    assertEquals("Error thrown for testing purposes", exceptionThrow.getMessage());
  }
}
