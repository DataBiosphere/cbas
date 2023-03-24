package bio.terra.cbas.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = MethodsApiController.class)
class TestMethodsApiControllerUnits {

  private static final String API = "/api/batch/v1/methods";
  @MockBean private MethodsApiController methodsApiController;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private CromwellService cromwellService;

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private MethodDao methodDao;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private RunSetDao runSetDao;

  @BeforeEach
  void instantiateMethodsController() {
    methodsApiController =
        new MethodsApiController(
            cromwellService, methodDao, methodVersionDao, runSetDao, objectMapper);
  }

  @Test
  void requestValidationForNullRequest() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "method_name is required",
                "method_source is required and should be one of: [GitHub]",
                "method_version is required",
                "method_url is required"));

    List<String> actualErrors = methodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationForEmptyRequest() {

    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    invalidPostRequest.setMethodName("");
    invalidPostRequest.setMethodDescription("this field is optional");
    invalidPostRequest.setMethodSource(MethodSourceEnum.GITHUB);
    invalidPostRequest.setMethodVersion("   ");
    invalidPostRequest.setMethodUrl("");
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "method_name is required", "method_version is required", "method_url is required"));

    List<String> actualErrors = methodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationForIncorrectRequest() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    invalidPostRequest.setMethodName("hello");
    invalidPostRequest.setMethodDescription("method description");
    invalidPostRequest.setMethodSource(MethodSourceEnum.GITHUB);
    invalidPostRequest.setMethodVersion("develop");
    invalidPostRequest.setMethodUrl("https://foo.net/abc/hello.wdl");
    List<String> expectedErrors =
        new ArrayList<>(
            List.of("method_url is invalid. Supported URI host(s): [raw.githubusercontent.com]"));

    List<String> actualErrors = methodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationWithIncorrectUrlFormat() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    invalidPostRequest.setMethodName("hello");
    invalidPostRequest.setMethodDescription("method description");
    invalidPostRequest.setMethodSource(MethodSourceEnum.GITHUB);
    invalidPostRequest.setMethodVersion("develop");
    invalidPostRequest.setMethodUrl("https://raw.githubusercontent/WDL/workflows/hello.wdl");
    List<String> expectedErrors =
        new ArrayList<>(List.of("method_url is invalid. URL doesn't match pattern format"));

    List<String> actualErrors = methodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationForValidRequest() {
    PostMethodRequest validPostRequest = new PostMethodRequest();
    validPostRequest.setMethodName("hello");
    validPostRequest.setMethodDescription("test hello method");
    validPostRequest.setMethodSource(MethodSourceEnum.GITHUB);
    validPostRequest.setMethodVersion("develop");
    validPostRequest.setMethodUrl(
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl");

    assertTrue(methodsApiController.validateMethod(validPostRequest).isEmpty());
  }
}
