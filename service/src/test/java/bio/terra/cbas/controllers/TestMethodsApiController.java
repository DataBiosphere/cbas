package bio.terra.cbas.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MicrometerMetrics;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.ecm.EcmService;
import bio.terra.cbas.dependencies.github.GitHubClient;
import bio.terra.cbas.dependencies.github.GitHubService;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.ErrorReport;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodLastRunDetails;
import bio.terra.cbas.model.MethodListResponse;
import bio.terra.cbas.model.MethodVersionDetails;
import bio.terra.cbas.model.PostMethodResponse;
import bio.terra.cbas.models.*;
import bio.terra.cbas.service.MethodService;
import bio.terra.cbas.service.MethodVersionService;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import bio.terra.dockstore.model.ToolDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.model.WorkflowDescription;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest
@ExtendWith({MockitoExtension.class})
@ContextConfiguration(classes = {MethodsApiController.class, GlobalExceptionHandler.class})
class TestMethodsApiController {

  private static final String API = "/api/batch/v1/methods";

  @MockBean private CromwellService cromwellService;
  @MockBean private DockstoreService dockstoreService;
  @MockBean private SamService samService;
  @MockBean private GitHubService gitHubService;
  @MockBean private MicrometerMetrics micrometerMetrics;

  // These mock beans are supplied to the RunSetApiController at construction time (and get used
  // later):
  @MockBean private MethodDao methodDao;
  @MockBean private MethodService methodService;
  @MockBean private MethodVersionDao methodVersionDao;
  @MockBean private MethodVersionService methodVersionService;
  @MockBean private RunSetDao runSetDao;
  @MockBean private EcmService ecmService;
  @MockBean private BearerTokenFactory bearerTokenFactory;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  // The object mapper is pulled from the BeanConfig and used to convert to and from JSON in the
  // tests:
  @Autowired private ObjectMapper objectMapper;
  @MockBean private CbasContextConfiguration cbasContextConfiguration;
  private static final UUID workspaceId = UUID.randomUUID();

  private void initSamMocks() {
    // setup Sam permission check to return true
    when(samService.hasReadPermission(any())).thenReturn(true);
    when(samService.hasWritePermission(any())).thenReturn(true);
  }

  // Set up the database query responses.
  // These answers are always the same, only the business logic that chooses them and interprets
  // the results should change:
  private void initMocks() {
    initSamMocks();

    Method neverRunMethod1WithGithub = neverRunMethod1.withGithubMethodDetails(githubMethodDetails);
    Method previouslyRunMethod2WithGithub =
        previouslyRunMethod2.withGithubMethodDetails(githubMethodDetails);

    when(methodDao.getMethods())
        .thenReturn(List.of(neverRunMethod1WithGithub, previouslyRunMethod2WithGithub));
    when(methodDao.getMethod(neverRunMethod1WithGithub.methodId()))
        .thenReturn(neverRunMethod1WithGithub);
    when(methodDao.getMethod(previouslyRunMethod2WithGithub.methodId()))
        .thenReturn(previouslyRunMethod2WithGithub);

    when(methodVersionDao.getMethodVersionsForMethod(neverRunMethod1WithGithub))
        .thenReturn(List.of(method1Version1, method1Version2));
    when(methodVersionDao.getMethodVersionsForMethod(previouslyRunMethod2WithGithub))
        .thenReturn(List.of(method2Version1, method2Version2));

    when(methodVersionDao.getMethodVersion(method1Version1.methodVersionId()))
        .thenReturn(method1Version1);
    when(methodVersionDao.getMethodVersion(method1Version2.methodVersionId()))
        .thenReturn(method1Version2);
    when(methodVersionDao.getMethodVersion(method2Version1.methodVersionId()))
        .thenReturn(method2Version1);
    when(methodVersionDao.getMethodVersion(method2Version2.methodVersionId()))
        .thenReturn(method2Version2);

    when(methodDao.methodLastRunDetailsFromRunSetIds(Set.of(method2RunSet1Id)))
        .thenReturn(Map.of(method2RunSet1Id, method2Version1RunsetDetails));
    when(methodDao.methodLastRunDetailsFromRunSetIds(Set.of(method2RunSet2Id)))
        .thenReturn(Map.of(method2RunSet2Id, method2Version2RunsetDetails));
    when(methodDao.methodLastRunDetailsFromRunSetIds(Set.of(method2RunSet1Id, method2RunSet2Id)))
        .thenReturn(
            Map.of(
                method2RunSet1Id, method2Version1RunsetDetails,
                method2RunSet2Id, method2Version2RunsetDetails));

    when(methodVersionService.methodVersionToMethodVersionDetails(method1Version1))
        .thenReturn(new MethodVersionDetails().lastRun(new MethodLastRunDetails()));

    when(methodVersionService.methodVersionToMethodVersionDetails(method1Version2))
        .thenReturn(new MethodVersionDetails().lastRun(new MethodLastRunDetails()));

    when(methodVersionService.methodVersionToMethodVersionDetails(method2Version1))
        .thenReturn(
            new MethodVersionDetails()
                .lastRun(method2Version1RunsetDetails)
                .description(method2Version1.description())
                .branchOrTagName(method2Version1.branchOrTagName()));

    when(methodVersionService.methodVersionToMethodVersionDetails(method2Version2))
        .thenReturn(new MethodVersionDetails().lastRun(new MethodLastRunDetails()));
  }

  @Test
  void returnAllMethodsAndVersions() throws Exception {
    initMocks();
    MvcResult result = mockMvc.perform(get(API)).andExpect(status().isOk()).andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(2, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod1 = parsedResponse.getMethods().get(0);
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(1);

    assertEquals(2, actualResponseForMethod1.getMethodVersions().size());
    assertEquals(2, actualResponseForMethod2.getMethodVersions().size());

    assertFalse(actualResponseForMethod1.getLastRun().isPreviouslyRun());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());

    assertFalse(actualResponseForMethod1.isIsPrivate());
    assertFalse(actualResponseForMethod2.isIsPrivate());
  }

  @Test
  void returnAllMethodsWithoutVersions() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(get(API).param("show_versions", "false"))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(2, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod1 = parsedResponse.getMethods().get(0);
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(1);

    assertNull(actualResponseForMethod1.getMethodVersions());
    assertNull(actualResponseForMethod2.getMethodVersions());

    assertFalse(actualResponseForMethod1.getLastRun().isPreviouslyRun());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());

    assertFalse(actualResponseForMethod1.isIsPrivate());
    assertFalse(actualResponseForMethod2.isIsPrivate());
  }

  @Test
  void returnSpecificMethod() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(get(API).param("method_id", previouslyRunMethod2.methodId().toString()))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(1, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(0);

    assertEquals(previouslyRunMethod2.methodId(), actualResponseForMethod2.getMethodId());
    assertEquals(2, actualResponseForMethod2.getMethodVersions().size());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());
    assertEquals(githubMethodDetails.isPrivate(), actualResponseForMethod2.isIsPrivate());
  }

  @Test
  void returnSpecificMethodWithoutVersion() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(
                get(API)
                    .param("method_id", previouslyRunMethod2.methodId().toString())
                    .param("show_versions", "false"))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(1, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(0);

    assertEquals(previouslyRunMethod2.methodId(), actualResponseForMethod2.getMethodId());
    assertNull(actualResponseForMethod2.getMethodVersions());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());
  }

  @Test
  void returnSpecificMethodVersion() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(
                get(API).param("method_version_id", method2Version1.methodVersionId().toString()))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(1, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(0);

    assertEquals(previouslyRunMethod2.methodId(), actualResponseForMethod2.getMethodId());
    assertEquals(1, actualResponseForMethod2.getMethodVersions().size());
    assertTrue(actualResponseForMethod2.getLastRun().isPreviouslyRun());

    var actualVersionDetails = actualResponseForMethod2.getMethodVersions().get(0);
    assertEquals(method2Version1.description(), actualVersionDetails.getDescription());
    assertTrue(actualVersionDetails.getLastRun().isPreviouslyRun());
    assertEquals(method2Version1Runset.runSetId(), actualVersionDetails.getLastRun().getRunSetId());
    assertEquals("develop", actualVersionDetails.getBranchOrTagName());
  }

  @Test
  void returnErrorForInvalidPostRequest() throws Exception {
    String invalidPostRequest =
        """
      {
        "method_name": "",
        "method_version":"",
        "method_url":"https://foo.net/abc/hello.wdl"
      }
      """;
    String expectedError =
        "Bad user request. Error(s): method_name is required. method_source is required and should be one of: [GitHub, Dockstore]. method_version is required";

    initSamMocks();

    MvcResult response =
        mockMvc
            .perform(post(API).content(invalidPostRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();
    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    assertNull(postMethodResponse.getMethodId());
    assertNull(postMethodResponse.getRunSetId());
    assertEquals(expectedError, postMethodResponse.getError());
  }

  @Test
  void returnErrorForInvalidGithubPostRequest() throws Exception {
    String invalidPostRequest =
        """
      {
        "method_name": "",
        "method_version":"develop",
        "method_source":"GitHub",
        "method_url":"https://foo.net/abc/hello.wdl"
      }
      """;
    String expectedError =
        "Bad user request. Error(s): method_name is required. method_url is invalid. Supported URI host(s): [github.com, raw.githubusercontent.com]";

    initSamMocks();

    MvcResult response =
        mockMvc
            .perform(post(API).content(invalidPostRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();
    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    assertNull(postMethodResponse.getMethodId());
    assertNull(postMethodResponse.getRunSetId());
    assertEquals(expectedError, postMethodResponse.getError());
  }

  @Test
  void returnErrorForInvalidWorkflowInPostRequest() throws Exception {
    initMocks();
    String invalidWorkflowRequest = postRequestTemplate.formatted("GitHub", invalidWorkflow);
    String expectedError =
        "Bad user request. Error(s): method_url is invalid. Github URL should be formatted like: <hostname> / <org> / <repo> / blob / <branch/tag/commit> / <path-to-file>";

    when(cromwellService.describeWorkflow(eq(invalidWorkflowWithCommit), any()))
        .thenReturn(workflowDescForInvalidWorkflow);
    when(gitHubService.getCurrentGithash(eq("org"), eq("repo"), eq("abc"), any()))
        .thenReturn(dummyHash);

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(invalidWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();

    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    assertNull(postMethodResponse.getMethodId());
    assertNull(postMethodResponse.getRunSetId());
    assertEquals(expectedError, postMethodResponse.getError());
  }

  @Test
  void returnErrorForInvalidMethodMappingsInPostRequest() throws Exception {
    initMocks();
    String invalidInputMapping =
        """
        [ {
          "input_name" : "wf_hello.hello.missing_addressee",
          "source" : {
            "type" : "record_lookup",
            "record_attribute": "addressee"
          }
        } ]
        """
            .trim();
    String invalidOutputMapping =
        """
        [ {
          "output_name" : "wf_hello.hello.missing_salutation",
          "destination" : {
            "type" : "record_update",
            "record_attribute": "salutation"
          }
        } ]
        """
            .trim();
    String expectedError =
        "Bad user request. Error(s): Invalid input mappings. '[wf_hello.hello.missing_addressee]' not found in workflow inputs. Invalid output mappings. '[wf_hello.hello.missing_salutation]' not found in workflow outputs.";

    String methodRequest =
        postRequestTemplateWithMappings.formatted(
            validRawWorkflow, invalidInputMapping, invalidOutputMapping);

    WorkflowDescription workflowDescForValidWorkflow =
        objectMapper.readValue(validWorkflowDescriptionJson, WorkflowDescription.class);
    when(cromwellService.describeWorkflow(eq(validRawWorkflowWithCommit), any()))
        .thenReturn(workflowDescForValidWorkflow);
    when(gitHubService.getCurrentGithash(
            eq("broadinstitute"), eq("cromwell"), eq("develop"), any()))
        .thenReturn(dummyHash);

    MvcResult response =
        mockMvc
            .perform(post(API).content(methodRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();

    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    assertNull(postMethodResponse.getMethodId());
    assertNull(postMethodResponse.getRunSetId());
    assertEquals(expectedError, postMethodResponse.getError());
  }

  @Test
  void validPostRequest() throws Exception {
    String validWorkflowRequest = postRequestTemplate.formatted("GitHub", validRawWorkflow);

    String expectedInput =
        """
        [ {
          "input_name" : "wf_hello.hello.addressee",
          "input_type" : {
            "type" : "primitive",
            "primitive_type" : "String"
          },
          "source" : {
            "type" : "none"
          }
        } ]
        """
            .trim();

    String expectedOutput =
        """
        [ {
          "output_name" : "wf_hello.hello.salutation",
          "output_type" : {
            "type" : "primitive",
            "primitive_type" : "String"
          },
          "destination" : {
            "type" : "none"
          }
        } ]
        """
            .trim();

    initSamMocks();
    WorkflowDescription workflowDescForValidWorkflow =
        objectMapper.readValue(validWorkflowDescriptionJson, WorkflowDescription.class);
    when(gitHubService.getCurrentGithash(
            eq("broadinstitute"), eq("cromwell"), eq("develop"), any()))
        .thenReturn(dummyHash);
    when(cromwellService.describeWorkflow(eq(validRawWorkflowWithCommit), any()))
        .thenReturn(workflowDescForValidWorkflow);
    when(gitHubService.isRepoPrivate(any(), any(), any())).thenReturn(true);

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    // verify records in method, method_version and run_set tables were passed arguments as expected
    ArgumentCaptor<Method> newMethodCaptor = ArgumentCaptor.forClass(Method.class);
    verify(methodDao).createMethod(newMethodCaptor.capture());
    assertEquals(postMethodResponse.getMethodId(), newMethodCaptor.getValue().methodId());
    assertEquals("test workflow", newMethodCaptor.getValue().name());
    assertEquals("GitHub", newMethodCaptor.getValue().methodSource());
    assertEquals("test method description", newMethodCaptor.getValue().description());
    assertNull(newMethodCaptor.getValue().lastRunSetId());

    ArgumentCaptor<MethodVersion> newMethodVersionCaptor =
        ArgumentCaptor.forClass(MethodVersion.class);
    verify(methodVersionDao).createMethodVersion(newMethodVersionCaptor.capture());
    assertEquals(postMethodResponse.getMethodId(), newMethodVersionCaptor.getValue().getMethodId());
    assertEquals("develop", newMethodVersionCaptor.getValue().name());
    assertEquals("test method description", newMethodVersionCaptor.getValue().description());
    assertEquals(validRawWorkflow, newMethodVersionCaptor.getValue().url());
    assertNull(newMethodVersionCaptor.getValue().lastRunSetId());
    assertEquals("develop", newMethodVersionCaptor.getValue().branchOrTagName());
    assertEquals(
        dummyHash, newMethodVersionCaptor.getValue().methodVersionDetails().get().githash());

    UUID methodVersionId = newMethodVersionCaptor.getValue().methodVersionId();
    ArgumentCaptor<RunSet> newRunSetCaptor = ArgumentCaptor.forClass(RunSet.class);
    verify(runSetDao).createRunSet(newRunSetCaptor.capture());
    assertEquals(postMethodResponse.getRunSetId(), newRunSetCaptor.getValue().runSetId());
    assertEquals(methodVersionId, newRunSetCaptor.getValue().getMethodVersionId());
    assertEquals("test workflow/develop workflow", newRunSetCaptor.getValue().name());
    assertEquals(
        "Template Run Set for Method test workflow/develop workflow",
        newRunSetCaptor.getValue().description());
    assertEquals(expectedInput, newRunSetCaptor.getValue().inputDefinition());
    assertEquals(expectedOutput, newRunSetCaptor.getValue().outputDefinition());
    assertTrue(newRunSetCaptor.getValue().isTemplate());

    assertEquals(
        postMethodResponse.getMethodId(),
        newMethodCaptor.getValue().githubMethodDetails().get().methodId());
    assertEquals(true, newMethodCaptor.getValue().githubMethodDetails().get().isPrivate());
  }

  @Test
  void validGithubMethodRequest() throws Exception {
    String validWorkflowRequest = postRequestTemplate.formatted("GitHub", validGithubWorkflow);

    initSamMocks();
    WorkflowDescription workflowDescForValidWorkflow =
        objectMapper.readValue(validWorkflowDescriptionJson, WorkflowDescription.class);
    when(gitHubService.getCurrentGithash(
            eq("broadinstitute"), eq("cromwell"), eq("develop"), any()))
        .thenReturn(dummyHash);
    when(cromwellService.describeWorkflow(eq(validRawWorkflowWithCommit), any()))
        .thenReturn(workflowDescForValidWorkflow);

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    assertNotNull(postMethodResponse.getMethodId());
    assertNotNull(postMethodResponse.getRunSetId());
    assertNull(postMethodResponse.getError());
  }

  @Test
  void validDockstoreMethodPostRequest() throws Exception {
    String validWorkflowRequest =
        postRequestTemplate.formatted("Dockstore", validDockstoreWorkflow);

    ToolDescriptor mockToolDescriptor = new ToolDescriptor();
    mockToolDescriptor.setDescriptor("mock descriptor");
    mockToolDescriptor.setType(ToolDescriptor.TypeEnum.WDL);
    mockToolDescriptor.setUrl(validRawWorkflow);

    initSamMocks();
    WorkflowDescription workflowDescForValidWorkflow =
        objectMapper.readValue(validWorkflowDescriptionJson, WorkflowDescription.class);
    when(dockstoreService.descriptorGetV1(validDockstoreWorkflow, "develop"))
        .thenReturn(mockToolDescriptor);
    when(cromwellService.describeWorkflow(eq(validRawWorkflow), any()))
        .thenReturn(workflowDescForValidWorkflow);

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    ArgumentCaptor<Method> createMethodCaptor = ArgumentCaptor.forClass(Method.class);
    verify(methodDao).createMethod(createMethodCaptor.capture());

    assertEquals(postMethodResponse.getMethodId(), createMethodCaptor.getValue().methodId());
    assertEquals(Optional.empty(), createMethodCaptor.getValue().githubMethodDetails());

    assertNotNull(postMethodResponse.getMethodId());
    assertNotNull(postMethodResponse.getRunSetId());
    assertNull(postMethodResponse.getError());
  }

  @Test
  void validMethodMappingPostRequest() throws Exception {
    initMocks();
    String validInputMapping =
        """
        [ {
          "input_name" : "wf_hello.hello.addressee",
          "source" : {
            "type" : "record_lookup",
            "record_attribute" : "addressee"
          }
        } ]
        """
            .trim();
    String validOutputMapping =
        """
        [ {
          "output_name" : "wf_hello.hello.salutation",
          "destination" : {
            "type" : "record_update",
            "record_attribute" : "salutation"
          }
        } ]
        """
            .trim();
    String expectedInputDefinition =
        """
        [ {
          "input_name" : "wf_hello.hello.addressee",
          "input_type" : {
            "type" : "primitive",
            "primitive_type" : "String"
          },
          "source" : {
            "type" : "record_lookup",
            "record_attribute" : "addressee"
          }
        } ]
        """
            .trim();
    String expectedOutputDefinition =
        """
        [ {
          "output_name" : "wf_hello.hello.salutation",
          "output_type" : {
            "type" : "primitive",
            "primitive_type" : "String"
          },
          "destination" : {
            "type" : "record_update",
            "record_attribute" : "salutation"
          }
        } ]
        """
            .trim();
    String validWorkflowRequest =
        postRequestTemplateWithMappings.formatted(
            validRawWorkflow, validInputMapping, validOutputMapping);

    WorkflowDescription workflowDescForValidWorkflow =
        objectMapper.readValue(validWorkflowDescriptionJson, WorkflowDescription.class);
    when(gitHubService.getCurrentGithash(
            eq("broadinstitute"), eq("cromwell"), eq("develop"), any()))
        .thenReturn(dummyHash);
    when(cromwellService.describeWorkflow(eq(validRawWorkflowWithCommit), any()))
        .thenReturn(workflowDescForValidWorkflow);

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    ArgumentCaptor<Method> newMethodCaptor = ArgumentCaptor.forClass(Method.class);
    verify(methodDao).createMethod(newMethodCaptor.capture());
    assertEquals(postMethodResponse.getMethodId(), newMethodCaptor.getValue().methodId());

    ArgumentCaptor<MethodVersion> newMethodVersionCaptor =
        ArgumentCaptor.forClass(MethodVersion.class);
    verify(methodVersionDao).createMethodVersion(newMethodVersionCaptor.capture());
    assertEquals(postMethodResponse.getMethodId(), newMethodVersionCaptor.getValue().getMethodId());

    // in this test we focus on only verifying that the input and output definitions have the method
    // mappings that were passed in the request
    UUID methodVersionId = newMethodVersionCaptor.getValue().methodVersionId();
    ArgumentCaptor<RunSet> newRunSetCaptor = ArgumentCaptor.forClass(RunSet.class);
    verify(runSetDao).createRunSet(newRunSetCaptor.capture());
    assertEquals(postMethodResponse.getRunSetId(), newRunSetCaptor.getValue().runSetId());
    assertEquals(methodVersionId, newRunSetCaptor.getValue().getMethodVersionId());
    assertEquals(expectedInputDefinition, newRunSetCaptor.getValue().inputDefinition());
    assertEquals(expectedOutputDefinition, newRunSetCaptor.getValue().outputDefinition());
    assertTrue(newRunSetCaptor.getValue().isTemplate());
  }

  @Test
  void requestValidationForDuplicateMethod() throws Exception {
    initMocks();

    String duplicateMethodRequest =
        """
      {
        "method_name": "method1",
        "method_description": "method one",
        "method_source":"GitHub",
        "method_version":"v1",
        "method_url": "%s"
      }
      """
            .formatted(validRawWorkflow);

    List<String> expectedErrors =
        new ArrayList<>(
            List.of(
                "Bad user request. Error(s): Method method1 already exists. Please select a new method."));

    when(methodDao.countMethods("method1", "v1")).thenReturn(1);

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(duplicateMethodRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();
    PostMethodResponse postMethodResponse =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), PostMethodResponse.class);

    assertEquals(expectedErrors.get(0), postMethodResponse.getError());
  }

  @Test
  void returnErrorForUserWithNoReadAccess() throws Exception {
    when(samService.hasReadPermission(any())).thenReturn(false);

    mockMvc
        .perform(get(API))
        .andExpect(status().isForbidden())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ForbiddenException))
        .andExpect(
            result ->
                assertEquals(
                    "User doesn't have 'read' permission on 'workspace' resource",
                    result.getResolvedException().getMessage()));
  }

  @Test
  void returnErrorForUserWithNoWriteAccess() throws Exception {
    String validWorkflowRequest = postRequestTemplate.formatted("GitHub", validRawWorkflow);

    when(samService.hasWritePermission(any())).thenReturn(false);

    mockMvc
        .perform(post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ForbiddenException))
        .andExpect(
            result ->
                assertEquals(
                    "User doesn't have 'write' permission on 'workspace' resource",
                    result.getResolvedException().getMessage()));
  }

  @Test
  void returnErrorForGetRequestWithoutToken() throws Exception {
    // call the real method that extracts the bearer token from request
    when(bearerTokenFactory.from(any())).thenCallRealMethod();

    MvcResult response =
        mockMvc
            .perform(get(API))
            .andExpect(status().isUnauthorized())
            .andExpect(
                result ->
                    assertTrue(result.getResolvedException() instanceof UnauthorizedException))
            .andReturn();

    // verify that the response object is of type ErrorReport and that the nested Unauthorized
    // exception was surfaced to user
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(401, errorResponse.getStatusCode());
    assertEquals("Authorization header missing", errorResponse.getMessage());
  }

  @Test
  void returnErrorForPostRequestWithoutToken() throws Exception {
    String validWorkflowRequest = postRequestTemplate.formatted("GitHub", validRawWorkflow);

    // call the real method that extracts the bearer token from request
    when(bearerTokenFactory.from(any())).thenCallRealMethod();

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(
                result ->
                    assertTrue(result.getResolvedException() instanceof UnauthorizedException))
            .andReturn();

    // verify that the response object is of type ErrorReport and that the nested Unauthorized
    // exception was surfaced to user
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(401, errorResponse.getStatusCode());
    assertEquals("Authorization header missing", errorResponse.getMessage());
  }

  @Test
  void returnErrorForSamApiException() throws Exception {
    // throw a form of ErrorReportException which is thrown when an ApiException happens in
    // hasPermission()
    when(samService.hasReadPermission(any()))
        .thenThrow(new SamUnauthorizedException("Exception thrown for testing purposes"));

    MvcResult response = mockMvc.perform(get(API)).andExpect(status().isUnauthorized()).andReturn();

    // verify that the response object is of type ErrorReport and that the exception message is set
    // properly
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(401, errorResponse.getStatusCode());
    assertEquals("Exception thrown for testing purposes", errorResponse.getMessage());
  }

  @Test
  void returnErrorForSamInterruptedException() throws Exception {
    String validWorkflowRequest = postRequestTemplate.formatted("GitHub", validRawWorkflow);

    // throw SamInterruptedException which is thrown when InterruptedException happens in
    // hasPermission()
    when(samService.hasWritePermission(any()))
        .thenThrow(new SamInterruptedException("InterruptedException thrown for testing purposes"));

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andReturn();

    // verify that the response object is of type ErrorReport and that the exception message is set
    // properly
    ErrorReport errorResponse =
        objectMapper.readValue(response.getResponse().getContentAsString(), ErrorReport.class);

    assertEquals(500, errorResponse.getStatusCode());
    assertEquals("InterruptedException thrown for testing purposes", errorResponse.getMessage());
  }

  @Test
  // the purpose of this test is to call the real method to extract the bearer token from request
  // and verify that hasReadPermission received the same bearer token set in request
  void testBearerTokenExtractionMethod() throws Exception {
    String userToken = "mock-user-token";

    when(bearerTokenFactory.from(any())).thenCallRealMethod();

    mockMvc.perform(get(API).header("Authorization", "Bearer %s".formatted(userToken)));

    ArgumentCaptor<BearerToken> bearerTokenCaptor = ArgumentCaptor.forClass(BearerToken.class);
    verify(samService).hasReadPermission(bearerTokenCaptor.capture());

    assertEquals(userToken, bearerTokenCaptor.getValue().getToken());
  }

  @Test
  void returnSpecificMethodWithDetails() throws Exception {
    initMocks();
    MvcResult result =
        mockMvc
            .perform(get(API).param("method_id", previouslyRunMethod2.methodId().toString()))
            .andExpect(status().isOk())
            .andReturn();

    var parsedResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), MethodListResponse.class);

    assertEquals(1, parsedResponse.getMethods().size());
    MethodDetails actualResponseForMethod2 = parsedResponse.getMethods().get(0);

    assertEquals(previouslyRunMethod2.methodId(), actualResponseForMethod2.getMethodId());
    assertEquals(false, actualResponseForMethod2.isIsPrivate());
  }

  @Test
  void errorThrownForInvalidToken() throws Exception {
    String validWorkflowRequest = postRequestTemplate.formatted("GitHub", validRawWorkflow);
    String errorResponse =
        """
        {
          "error" : "Error while importing GitHub workflow. Error: exception thrown"
        }
        """
            .trim();
    initSamMocks();
    WorkflowDescription workflowDescForValidWorkflow =
        objectMapper.readValue(validWorkflowDescriptionJson, WorkflowDescription.class);
    when(cromwellService.describeWorkflow(eq(validRawWorkflow), any()))
        .thenReturn(workflowDescForValidWorkflow);
    when(gitHubService.isRepoPrivate(any(), any(), any()))
        .thenThrow(new GitHubClient.GitHubClientException("exception thrown"));

    MvcResult response =
        mockMvc
            .perform(
                post(API).content(validWorkflowRequest).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(errorResponse))
            .andReturn();

    System.out.println(response.getResponse().getContentAsString());
  }

  private static final Method neverRunMethod1 =
      new Method(
          UUID.randomUUID(),
          "method1",
          "method one",
          OffsetDateTime.now(),
          null,
          "method 1 source",
          workspaceId,
          Optional.empty(),
          CbasMethodStatus.ACTIVE);

  private static final MethodVersion method1Version1 =
      new MethodVersion(
          UUID.randomUUID(),
          neverRunMethod1,
          "v1",
          "method one version 1",
          OffsetDateTime.now(),
          null,
          "file://method1/v1.wdl",
          workspaceId,
          "develop",
          Optional.empty());

  private static final MethodVersion method1Version2 =
      new MethodVersion(
          UUID.randomUUID(),
          neverRunMethod1,
          "v2",
          "method one version 2",
          OffsetDateTime.now(),
          null,
          "file://method1/v2.wdl",
          workspaceId,
          "develop",
          Optional.empty());

  private static final UUID method2RunSet1Id = UUID.randomUUID();
  private static final UUID method2RunSet2Id = UUID.randomUUID();
  private static final String dummyHash = "dummy_hash";
  private static final String invalidWorkflow =
      "https://raw.githubusercontent.com/org/repo/abc/invalidWorkflow.wdl";
  private static final String invalidWorkflowWithCommit =
      "https://raw.githubusercontent.com/org/repo/%s/invalidWorkflow.wdl".formatted(dummyHash);
  private static final String validRawWorkflow =
      "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl";
  private static final String validRawWorkflowWithCommit =
      "https://raw.githubusercontent.com/broadinstitute/cromwell/%s/centaur/src/main/resources/standardTestCases/hello/hello.wdl"
          .formatted(dummyHash);
  private static final String validGithubWorkflow =
      "https://github.com/broadinstitute/cromwell/blob/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl";
  private static final String validDockstoreWorkflow =
      "github.com/broadinstitute/cromwell/hello.wdl";
  private static final Method previouslyRunMethod2 =
      new Method(
          UUID.randomUUID(),
          "method2",
          "method two",
          OffsetDateTime.now(),
          method2RunSet2Id,
          "method 2 source",
          workspaceId,
          Optional.empty(),
          CbasMethodStatus.ACTIVE);

  private static final GithubMethodDetails githubMethodDetails =
      new GithubMethodDetails(
          "cromwell",
          "broadinstitute",
          "/blob/path/azure/file.sh",
          false,
          previouslyRunMethod2.methodId());

  private static final UUID method2Version1VersionID = UUID.randomUUID();
  private static final MethodVersion method2Version1 =
      new MethodVersion(
          method2Version1VersionID,
          previouslyRunMethod2,
          "v1",
          "method two version 1",
          OffsetDateTime.now(),
          method2RunSet1Id,
          "file://method2/v1.wdl",
          workspaceId,
          "develop",
          Optional.empty());

  private static final UUID method2Version2VersionID = UUID.randomUUID();
  private static final MethodVersion method2Version2 =
      new MethodVersion(
          method2Version2VersionID,
          previouslyRunMethod2,
          "v2",
          "method two version 2",
          OffsetDateTime.now(),
          method2RunSet2Id,
          "file://method2/v2.wdl",
          workspaceId,
          "develop",
          Optional.empty());

  private static final RunSet method2Version1Runset =
      new RunSet(
          method2RunSet1Id,
          method2Version1,
          "Run workflow 2 v1",
          "Run workflow 2 v1",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          100,
          0,
          "[]",
          "[]",
          "FOO",
          "user-foo",
          workspaceId);

  private static final MethodLastRunDetails method2Version1RunsetDetails =
      new MethodLastRunDetails()
          .previouslyRun(true)
          .timestamp(DateUtils.convertToDate(OffsetDateTime.now()))
          .methodVersionName("method two version 1")
          .methodVersionId(method2Version1VersionID)
          .runSetId(method2RunSet1Id);

  private static final RunSet method2Version2Runset =
      new RunSet(
          method2RunSet2Id,
          method2Version2,
          "Run workflow 2 v1, take 2",
          "Run workflow 2 v1 a second time",
          false,
          false,
          CbasRunSetStatus.COMPLETE,
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          OffsetDateTime.now(),
          100,
          0,
          "[]",
          "[]",
          "FOO",
          "user-foo",
          workspaceId);

  private static final MethodLastRunDetails method2Version2RunsetDetails =
      new MethodLastRunDetails()
          .previouslyRun(true)
          .timestamp(DateUtils.convertToDate(method2Version2Runset.submissionTimestamp()))
          .methodVersionName(method2Version2Runset.methodVersion().name())
          .methodVersionId(method2Version2Runset.getMethodVersionId())
          .runSetId(method2Version2Runset.runSetId());
  private final String postRequestTemplate =
      """
      {
        "method_name": "test workflow",
        "method_description": "test method description",
        "method_source":"%s",
        "method_version":"develop",
        "method_url": "%s"
      }
      """;

  private final String postRequestTemplateWithMappings =
      """
      {
        "method_name": "test workflow",
        "method_description": "test method description",
        "method_source":"GitHub",
        "method_version":"develop",
        "method_url": "%s",
        "method_input_mappings": %s,
        "method_output_mappings": %s
      }
      """;

  private final String validWorkflowDescriptionJson =
      """
      {
        "valid": true,
        "errors": [],
        "validWorkflow": true,
        "name": "wf_hello",
        "inputs": [
          {
            "name": "hello.addressee",
            "valueType": {
              "typeName": "STRING"
            },
            "typeDisplayName": "String",
            "optional": false,
            "default": null
          }
        ],
        "outputs": [
          {
            "name": "hello.salutation",
            "valueType": {
              "typeName": "STRING"
            },
            "typeDisplayName": "String"
          }
        ],
        "images": [
          "ubuntu@sha256:71cd81252a3563a03ad8daee81047b62ab5d892ebbfbf71cf53415f29c130950"
        ],
        "submittedDescriptorType": {
          "descriptorType": "WDL",
          "descriptorTypeVersion": "draft-2"
        },
        "importedDescriptorTypes": [],
        "meta": {},
        "parameterMeta": {},
        "isRunnableWorkflow": true
      }
      """
          .stripIndent()
          .trim();

  private static final WorkflowDescription workflowDescForInvalidWorkflow =
      new WorkflowDescription().valid(false).errors(List.of("Workflow invalid for test purposes"));
}
