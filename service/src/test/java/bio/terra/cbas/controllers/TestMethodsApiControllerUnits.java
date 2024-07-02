package bio.terra.cbas.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.github.GitHubService;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.MethodInputMapping;
import bio.terra.cbas.model.MethodOutputMapping;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum;
import bio.terra.cbas.service.MethodService;
import bio.terra.cbas.service.MethodVersionService;
import bio.terra.common.iam.BearerTokenFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.WorkflowDescription;
import jakarta.servlet.http.HttpServletRequest;
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
  private static final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  @MockBean private MethodsApiController methodsApiController;

  @Autowired private MockMvc mockMvc;
  @MockBean private CromwellService cromwellService;
  @MockBean private DockstoreService dockstoreService;
  @MockBean private GitHubService gitHubService;
  @MockBean private SamService samService;

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private MethodDao methodDao;
  @MockBean private MethodService methodService;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private MethodVersionService methodVersionService;
  @MockBean private RunSetDao runSetDao;
  @MockBean private CbasContextConfiguration cbasContextConfiguration;
  @MockBean private BearerTokenFactory bearerTokenFactory;
  @MockBean private HttpServletRequest httpServletRequest;
  @MockBean private MicrometerMetrics micrometerMetrics;

  String workflowDescriptionString =
      """
      {
        "valid": true,
        "errors": [],
        "validWorkflow": true,
        "name": "hello_world",
        "inputs": [
          {
            "name": "foo",
            "valueType": {
              "typeName": "STRING"
            },
            "typeDisplayName": "String",
            "optional": false,
            "default": null
          },
          {
            "name": "bar",
            "valueType": {
              "typeName": "STRING"
            },
            "typeDisplayName": "String",
            "optional": true,
            "default": "hello"
          }
        ],
        "outputs": [
          {
            "name": "foo_rating",
            "valueType": {
              "typeName": "STRING"
            },
            "typeDisplayName": "String"
          },
          {
            "name": "bar_rating",
            "valueType": {
              "typeName": "STRING"
            },
            "typeDisplayName": "String"
          }
        ],
        "isRunnableWorkflow": true
      }
      """;
  String validInputMappingString =
      """
      [
        {
          "input_name": "hello_world.foo",
          "source": {
            "type": "record_lookup",
            "record_attribute": "foo_id"
          }
        }
      ]
      """;
  String invalidInputMappingString =
      """
      [
        {
          "input_name": "hello_world.wrong_foo",
          "source": {
            "type": "record_lookup",
            "record_attribute": "foo_id"
          }
        }
      ]
      """;
  String validOutputMappingString =
      """
      [
        {
          "output_name": "hello_world.foo_rating",
          "destination": {
            "type": "record_update",
            "record_attribute": "foo_rating"
          }
        }
      ]
      """;
  String invalidOutputMappingString =
      """
      [
        {
          "output_name": "hello_world.wrong_foo_rating",
          "destination": {
            "type": "record_update",
            "record_attribute": "foo_rating"
          }
        }
      ]
      """;

  WorkflowDescription workflowDescription =
      objectMapper.readValue(workflowDescriptionString, new TypeReference<>() {});
  List<MethodInputMapping> validInputMappings =
      objectMapper.readValue(validInputMappingString, new TypeReference<>() {});
  List<MethodInputMapping> inValidInputMappings =
      objectMapper.readValue(invalidInputMappingString, new TypeReference<>() {});
  List<MethodOutputMapping> validOutputMappings =
      objectMapper.readValue(validOutputMappingString, new TypeReference<>() {});
  List<MethodOutputMapping> inValidOutputMappings =
      objectMapper.readValue(invalidOutputMappingString, new TypeReference<>() {});

  TestMethodsApiControllerUnits() throws JsonProcessingException {}

  @BeforeEach
  void instantiateMethodsController() {
    methodsApiController =
        new MethodsApiController(
            cromwellService,
            dockstoreService,
            gitHubService,
            samService,
            methodDao,
            methodService,
            methodVersionDao,
            methodVersionService,
            runSetDao,
            objectMapper,
            cbasContextConfiguration,
            bearerTokenFactory,
            httpServletRequest,
            micrometerMetrics);
  }

  @Test
  void requestValidationForNullRequest() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "method_name is required",
                "method_source is required and should be one of: [GitHub, Dockstore]",
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
            List.of(
                "method_url is invalid. Supported URI host(s): [github.com, raw.githubusercontent.com]"));

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
        new ArrayList<>(
            List.of(
                "method_url is invalid. Supported URI host(s): [github.com, raw.githubusercontent.com]"));

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

  @Test
  void methodMappingValidationForIncorrectInputMapping() {
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "Invalid input mappings. '[hello_world.wrong_foo]' not found in workflow inputs."));
    assertEquals(
        expectedErrors,
        methodsApiController.validateMethodMappings(
            workflowDescription, inValidInputMappings, new ArrayList<>()));
  }

  @Test
  void methodMappingValidationForIncorrectOutputMapping() {
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "Invalid output mappings. '[hello_world.wrong_foo_rating]' not found in workflow outputs."));
    assertEquals(
        expectedErrors,
        methodsApiController.validateMethodMappings(
            workflowDescription, new ArrayList<>(), inValidOutputMappings));
  }

  @Test
  void methodMappingValidationForIncorrectMappings() {
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "Invalid input mappings. '[hello_world.wrong_foo]' not found in workflow inputs.",
                "Invalid output mappings. '[hello_world.wrong_foo_rating]' not found in workflow outputs."));
    assertEquals(
        expectedErrors,
        methodsApiController.validateMethodMappings(
            workflowDescription, inValidInputMappings, inValidOutputMappings));
  }

  @Test
  void methodValidationForCorrectMappings() {
    List<String> actualErrors =
        methodsApiController.validateMethodMappings(
            workflowDescription, validInputMappings, validOutputMappings);
    assertEquals(0, actualErrors.size());
  }
}
