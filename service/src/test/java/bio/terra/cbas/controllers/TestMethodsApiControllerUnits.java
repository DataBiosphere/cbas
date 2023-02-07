package bio.terra.cbas.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cbas.model.PostMethodRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestMethodsApiControllerUnits {

  @Test
  void requestValidationForNullRequest() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "method_name is required",
                "method_source is required",
                "method_version is required",
                "method_url is required"));

    List<String> actualErrors = MethodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationForEmptyRequest() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    invalidPostRequest.setMethodName("");
    invalidPostRequest.setMethodDescription("this field is optional");
    invalidPostRequest.setMethodSource(" ");
    invalidPostRequest.setMethodVersion("   ");
    invalidPostRequest.setMethodUrl("");
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "method_name is required",
                "method_source is required",
                "method_version is required",
                "method_url is required"));

    List<String> actualErrors = MethodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationForIncorrectRequest() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    invalidPostRequest.setMethodName("hello");
    invalidPostRequest.setMethodDescription("method description");
    invalidPostRequest.setMethodSource("FOO");
    invalidPostRequest.setMethodVersion("develop");
    invalidPostRequest.setMethodUrl("https://foo.net/abc/hello.wdl");
    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "method_source is invalid. Supported source(s): [GitHub]",
                "method_url is invalid. Supported URI host(s): [raw.githubusercontent.com]"));

    List<String> actualErrors = MethodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationShouldIgnoreSourceCase() {
    PostMethodRequest invalidPostRequest = new PostMethodRequest();
    invalidPostRequest.setMethodName("hello");
    invalidPostRequest.setMethodSource("github");
    invalidPostRequest.setMethodVersion("develop");
    invalidPostRequest.setMethodUrl("https://github.com/abc/hello.wdl");
    List<String> expectedErrors =
        new ArrayList<>(
            List.of("method_url is invalid. Supported URI host(s): [raw.githubusercontent.com]"));

    List<String> actualErrors = MethodsApiController.validateMethod(invalidPostRequest);
    assertEquals(expectedErrors.size(), actualErrors.size());
    assertIterableEquals(expectedErrors, actualErrors);
  }

  @Test
  void requestValidationForValidRequest() {
    PostMethodRequest validPostRequest = new PostMethodRequest();
    validPostRequest.setMethodName("hello");
    validPostRequest.setMethodDescription("test hello method");
    validPostRequest.setMethodSource("github");
    validPostRequest.setMethodVersion("develop");
    validPostRequest.setMethodUrl(
        "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl");

    assertTrue(MethodsApiController.validateMethod(validPostRequest).isEmpty());
  }
}
