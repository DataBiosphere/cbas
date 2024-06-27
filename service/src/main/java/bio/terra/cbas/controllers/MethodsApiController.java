package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MetricsUtil.increaseEventCounter;
import static bio.terra.cbas.common.MetricsUtil.recordMethodCreationCompletion;
import static bio.terra.cbas.dependencies.github.GitHubService.buildRawGithubUrl;
import static bio.terra.cbas.dependencies.github.GitHubService.validateGithubUrl;
import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.DOCKSTORE;
import static bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum.GITHUB;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womToCbasInputBuilder;
import static bio.terra.cbas.util.methods.WomtoolToCbasInputsAndOutputs.womToCbasOutputBuilder;

import bio.terra.cbas.api.MethodsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MethodUtil;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.common.exceptions.MethodProcessingException;
import bio.terra.cbas.common.exceptions.WomtoolValueTypeProcessingException.WomtoolValueTypeNotFoundException;
import bio.terra.cbas.common.validation.CbasValidationError;
import bio.terra.cbas.common.validation.CbasVoidValidation;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.github.GitHubClient;
import bio.terra.cbas.dependencies.github.GitHubService;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodInputMapping;
import bio.terra.cbas.model.MethodListResponse;
import bio.terra.cbas.model.MethodOutputMapping;
import bio.terra.cbas.model.MethodVersionDetails;
import bio.terra.cbas.model.PatchMethodRequest;
import bio.terra.cbas.model.PatchMethodResponse;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PostMethodRequest.MethodSourceEnum;
import bio.terra.cbas.model.PostMethodResponse;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.model.WorkflowOutputDefinition;
import bio.terra.cbas.models.CbasMethodStatus;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.service.MethodService;
import bio.terra.cbas.service.MethodVersionService;
import bio.terra.cbas.util.methods.GithubUrlComponents;
import bio.terra.common.exception.BadRequestException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cromwell.client.ApiException;
import cromwell.client.model.WorkflowDescription;
import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClientException;

@Controller
public class MethodsApiController implements MethodsApi {
  private final CromwellService cromwellService;
  private final DockstoreService dockstoreService;
  private final GitHubService gitHubService;
  private final SamService samService;
  private final MethodDao methodDao;
  private final MethodService methodService;
  private final MethodVersionDao methodVersionDao;
  private final MethodVersionService methodVersionService;
  private final RunSetDao runSetDao;
  private final CbasContextConfiguration cbasContextConfig;
  private final BearerTokenFactory bearerTokenFactory;
  private final HttpServletRequest httpServletRequest;

  public MethodsApiController(
      CromwellService cromwellService,
      DockstoreService dockstoreService,
      GitHubService gitHubService,
      SamService samService,
      MethodDao methodDao,
      MethodService methodService,
      MethodVersionDao methodVersionDao,
      MethodVersionService methodVersionService,
      RunSetDao runSetDao,
      ObjectMapper objectMapper,
      CbasContextConfiguration cbasContextConfig,
      BearerTokenFactory bearerTokenFactory,
      HttpServletRequest httpServletRequest) {
    this.cromwellService = cromwellService;
    this.dockstoreService = dockstoreService;
    this.gitHubService = gitHubService;
    this.samService = samService;
    this.methodDao = methodDao;
    this.methodService = methodService;
    this.methodVersionDao = methodVersionDao;
    this.methodVersionService = methodVersionService;
    this.runSetDao = runSetDao;
    this.objectMapper = objectMapper;
    this.cbasContextConfig = cbasContextConfig;
    this.bearerTokenFactory = bearerTokenFactory;
    this.httpServletRequest = httpServletRequest;
  }

  private final ObjectMapper objectMapper;

  @Override
  public ResponseEntity<PostMethodResponse> postMethod(PostMethodRequest postMethodRequest) {
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    // check if current user has write permissions on the workspace
    if (!samService.hasWritePermission(userToken)) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    long requestStartNanos = System.nanoTime();

    // validate request
    List<String> validationErrors = validateMethod(postMethodRequest);
    if (!validationErrors.isEmpty()) {
      String errorMsg = "Bad user request. Error(s): " + String.join(". ", validationErrors);
      log.warn(errorMsg);
      return new ResponseEntity<>(new PostMethodResponse().error(errorMsg), HttpStatus.BAD_REQUEST);
    }

    MethodSourceEnum methodSource = postMethodRequest.getMethodSource();
    String methodUrl = postMethodRequest.getMethodUrl();
    String methodVersionString = postMethodRequest.getMethodVersion();

    UUID methodId = UUID.randomUUID();
    UUID methodVersionId = UUID.randomUUID();
    UUID runSetId = UUID.randomUUID();

    String branchOrTagName = null;
    GithubMethodDetails githubMethodDetails = null;
    Optional<GithubMethodVersionDetails> githubMethodVersionDetails = Optional.empty();

    // resolve url to its raw/pinned form and use that to call Cromwell's /describe endpoint
    String resolvedMethodUrl;
    try {
      resolvedMethodUrl =
          switch (methodSource) {
            case DOCKSTORE -> dockstoreService
                .descriptorGetV1(methodUrl, methodVersionString)
                .getUrl();
            case GITHUB -> {
              GithubUrlComponents githubUrlComponents =
                  MethodUtil.extractGithubUrlComponents(methodUrl);
              branchOrTagName = githubUrlComponents.branchOrTag();
              Boolean isPrivate =
                  gitHubService.isRepoPrivate(
                      githubUrlComponents.org(), githubUrlComponents.repo(), userToken);
              githubMethodDetails =
                  new GithubMethodDetails(
                      githubUrlComponents.repo(),
                      githubUrlComponents.org(),
                      githubUrlComponents.path(),
                      isPrivate,
                      methodId);

              String gitHash =
                  gitHubService.getCurrentGithash(
                      githubUrlComponents.org(),
                      githubUrlComponents.repo(),
                      branchOrTagName,
                      userToken);

              githubMethodVersionDetails =
                  Optional.of(new GithubMethodVersionDetails(gitHash, methodVersionId));

              yield buildRawGithubUrl(
                  githubUrlComponents.org(),
                  githubUrlComponents.repo(),
                  gitHash,
                  githubUrlComponents.path());
            }
          };
    } catch (URISyntaxException | MalformedURLException | MethodProcessingException e) {
      String errorMsg =
          "Bad user request. Method url has invalid value. Error: %s".formatted(e.getMessage());
      log.warn(errorMsg, e);
      return new ResponseEntity<>(new PostMethodResponse().error(errorMsg), HttpStatus.BAD_REQUEST);
    } catch (bio.terra.dockstore.client.ApiException e) {
      String errorMsg =
          "Error while importing Dockstore workflow. Error: %s".formatted(e.getMessage());
      log.warn(errorMsg, e);
      increaseEventCounter("Dockstore method import error", 1);
      return new ResponseEntity<>(new PostMethodResponse().error(errorMsg), HttpStatus.BAD_REQUEST);
    } catch (GitHubClient.GitHubClientException e) {
      String errorMsg =
          "Error while importing GitHub workflow. Error: %s".formatted(e.getMessage());
      log.warn(errorMsg, e);
      increaseEventCounter("GitHub method import error", 1);
      return new ResponseEntity<>(new PostMethodResponse().error(errorMsg), HttpStatus.BAD_REQUEST);
    }

    // this could happen if there was no url,
    // or if an empty url was received in the Dockstore workflow's descriptor response.
    if (resolvedMethodUrl == null || resolvedMethodUrl.isEmpty()) {
      String errorMsg = "Error while importing workflow. No workflow URL found at specified path.";
      log.warn(errorMsg);
      return new ResponseEntity<>(new PostMethodResponse().error(errorMsg), HttpStatus.BAD_REQUEST);
    }

    // call Cromwell's /describe endpoint to get description of the workflow,
    // along with inputs and outputs.
    WorkflowDescription workflowDescription;
    try {
      workflowDescription = cromwellService.describeWorkflow(resolvedMethodUrl, userToken);

      // return 400 if method is invalid
      if (!workflowDescription.getValid()) {
        String invalidMethodErrors =
            String.format(
                "Bad user request. Method '%s' (resolved to %s) is invalid. Error(s): %s",
                methodUrl, resolvedMethodUrl, String.join(". ", workflowDescription.getErrors()));
        log.warn(invalidMethodErrors);
        recordMethodCreationCompletion(
            methodSource.toString(), HttpStatus.BAD_REQUEST.value(), requestStartNanos);

        return new ResponseEntity<>(
            new PostMethodResponse().error(invalidMethodErrors), HttpStatus.BAD_REQUEST);
      }

      // validate that passed input and output mappings exist in workflow
      List<MethodInputMapping> methodInputMappings = postMethodRequest.getMethodInputMappings();
      List<MethodOutputMapping> methodOutputMappings = postMethodRequest.getMethodOutputMappings();
      List<String> invalidMappingErrors =
          validateMethodMappings(workflowDescription, methodInputMappings, methodOutputMappings);

      // return 400 if input and/or output mappings is invalid
      if (!invalidMappingErrors.isEmpty()) {
        String invalidMappingError =
            String.format("Bad user request. Error(s): %s", String.join(" ", invalidMappingErrors));
        log.warn(invalidMappingError);
        recordMethodCreationCompletion(
            methodSource.toString(), HttpStatus.BAD_REQUEST.value(), requestStartNanos);

        return new ResponseEntity<>(
            new PostMethodResponse().error(invalidMappingError), HttpStatus.BAD_REQUEST);
      }

      // store method in database along with input and output definitions

      createNewMethod(
          methodId,
          methodVersionId,
          runSetId,
          postMethodRequest,
          workflowDescription,
          methodInputMappings,
          methodOutputMappings,
          githubMethodDetails,
          githubMethodVersionDetails,
          branchOrTagName);

      recordMethodCreationCompletion(
          methodSource.toString(), HttpStatus.OK.value(), requestStartNanos);
      PostMethodResponse postMethodResponse =
          new PostMethodResponse().methodId(methodId).runSetId(runSetId);

      return new ResponseEntity<>(postMethodResponse, HttpStatus.OK);
    } catch (ApiException
        | JsonProcessingException
        | WomtoolValueTypeNotFoundException
        | RestClientException e) {
      String errorMsg =
          String.format(
              "Something went wrong while importing the method '%s'. Error(s): %s",
              postMethodRequest.getMethodUrl(), e.getMessage());
      log.warn(errorMsg);
      recordMethodCreationCompletion(
          methodSource.toString(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestStartNanos);

      return new ResponseEntity<>(
          new PostMethodResponse().error(errorMsg), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public ResponseEntity<MethodListResponse> getMethods(
      Boolean showVersions, UUID methodId, UUID methodVersionId) {
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    // check if current user has read permissions on the workspace
    if (!samService.hasReadPermission(userToken)) {
      throw new ForbiddenException(SamService.READ_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    List<MethodDetails> methodDetails;
    if (methodVersionId != null) {
      try {
        methodDetails =
            List.of(
                methodVersionToMethodDetails(methodVersionDao.getMethodVersion(methodVersionId)));
      } catch (bio.terra.cbas.common.exceptions.MethodProcessingException
          | java.net.MalformedURLException
          | java.net.URISyntaxException
          | bio.terra.dockstore.client.ApiException e) {
        return ResponseEntity.badRequest().build();
      }
    } else {
      List<Method> methods =
          methodId == null ? methodDao.getMethods() : List.of(methodDao.getMethod(methodId));
      boolean nullSafeShowVersions = showVersions == null || showVersions;

      methodDetails =
          methods.stream().map(m -> methodToMethodDetails(m, nullSafeShowVersions)).toList();
    }

    addLastRunDetails(methodDetails);
    return ResponseEntity.ok(new MethodListResponse().methods(methodDetails));
  }

  @Override
  public ResponseEntity<PatchMethodResponse> patchMethod(
      UUID methodId, PatchMethodRequest request) {
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    // check if current user has write permissions on the workspace
    if (!samService.hasWritePermission(userToken)) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    if (request.getMethodStatus() != null
        && request.getMethodStatus().equals(PatchMethodRequest.MethodStatusEnum.ARCHIVED)) {
      methodService.archiveMethod(methodId);
      return ResponseEntity.ok(new PatchMethodResponse().methodId(methodId));
    } else {
      throw new BadRequestException(
          "Bad Request: PATCH only supports updating 'method_status' to 'ARCHIVED' }");
    }
  }

  private void createNewMethod(
      UUID methodId,
      UUID methodVersionId,
      UUID runSetId,
      PostMethodRequest postMethodRequest,
      WorkflowDescription workflowDescription,
      List<MethodInputMapping> methodInputMappings,
      List<MethodOutputMapping> methodOutputMappings,
      GithubMethodDetails githubMethodDetails,
      Optional<GithubMethodVersionDetails> githubMethodVersionDetails,
      String branchOrTagName)
      throws WomtoolValueTypeNotFoundException, JsonProcessingException {

    // convert WomTool inputs and outputs schema to CBAS input and output definition
    List<WorkflowInputDefinition> inputs =
        womToCbasInputBuilder(workflowDescription, methodInputMappings);
    List<WorkflowOutputDefinition> outputs =
        womToCbasOutputBuilder(workflowDescription, methodOutputMappings);

    // because of FK constraints, lastRunSetId for method and method_version will be added
    // after the run set is created
    Method method =
        new Method(
            methodId,
            postMethodRequest.getMethodName(),
            postMethodRequest.getMethodDescription(),
            DateUtils.currentTimeInUTC(),
            null,
            postMethodRequest.getMethodSource().toString(),
            cbasContextConfig.getWorkspaceId(),
            Optional.ofNullable(githubMethodDetails),
            CbasMethodStatus.ACTIVE);

    MethodVersion methodVersion =
        new MethodVersion(
            methodVersionId,
            method,
            postMethodRequest.getMethodVersion(),
            method.description(),
            DateUtils.currentTimeInUTC(),
            null,
            postMethodRequest.getMethodUrl(),
            cbasContextConfig.getWorkspaceId(),
            branchOrTagName,
            githubMethodVersionDetails);

    String templateRunSetName =
        String.format("%s/%s workflow", method.name(), methodVersion.name());
    String templateRunSetDesc = "Template Run Set for Method " + templateRunSetName;
    RunSet templateRunSet =
        new RunSet(
            runSetId,
            methodVersion,
            templateRunSetName,
            templateRunSetDesc,
            true, // callCachingEnabled
            true, // isTemplate
            CbasRunSetStatus.COMPLETE,
            DateUtils.currentTimeInUTC(),
            DateUtils.currentTimeInUTC(),
            DateUtils.currentTimeInUTC(),
            0,
            0,
            objectMapper.writeValueAsString(inputs),
            objectMapper.writeValueAsString(outputs),
            null,
            null,
            cbasContextConfig.getWorkspaceId());

    methodDao.createMethod(method);
    methodVersionDao.createMethodVersion(methodVersion);
    runSetDao.createRunSet(templateRunSet);
  }

  public List<String> validateMethod(PostMethodRequest methodRequest) {
    String methodName = methodRequest.getMethodName();
    String methodVersion = methodRequest.getMethodVersion();
    String methodUrl = methodRequest.getMethodUrl();
    List<String> errors = new ArrayList<>();

    if (methodName == null || methodName.trim().isEmpty()) {
      errors.add("method_name is required");
    }

    if (methodRequest.getMethodSource() == null) {
      errors.add(
          "method_source is required and should be one of: "
              + Arrays.toString(MethodSourceEnum.values()));
    }

    if (methodVersion == null || methodVersion.trim().isEmpty()) {
      errors.add("method_version is required");
    }

    if (methodUrl == null || methodUrl.trim().isEmpty()) {
      errors.add("method_url is required");
    } else {
      // we only verify if URL is valid for GitHub source here. For Dockstore methods a workflow
      // path which is not completely a valid URL is sent as method url, and it's validity is
      // checked while fetching the raw GitHub url for the workflow path
      if (methodRequest.getMethodSource() == GITHUB) {
        CbasVoidValidation urlValidation = validateGithubUrl(methodUrl);
        if (urlValidation instanceof CbasValidationError urlErrors)
          errors.addAll(urlErrors.errors());
      }
    }

    int methodDbQuery = methodDao.countMethods(methodName, methodVersion);
    if (methodDbQuery != 0) {
      errors.add("Method %s already exists. Please select a new method.".formatted(methodName));
    }

    return errors;
  }

  public List<String> validateMethodMappings(
      WorkflowDescription workflowDescription,
      List<MethodInputMapping> methodInputMappings,
      List<MethodOutputMapping> methodOutputMappings) {
    List<String> invalidInputMappings = new ArrayList<>();
    List<String> invalidOutputMappings = new ArrayList<>();
    List<String> invalidMappingErrors = new ArrayList<>();
    String workflowName = workflowDescription.getName();

    // generate list of input names from input method mappings that are invalid
    if (methodInputMappings != null) {
      List<String> workflowInputNames =
          workflowDescription.getInputs().stream()
              .map(i -> "%s.%s".formatted(workflowName, i.getName()))
              .toList();
      for (MethodInputMapping inputMapping : methodInputMappings) {
        if (!workflowInputNames.contains(inputMapping.getInputName())) {
          invalidInputMappings.add(inputMapping.getInputName());
        }
      }
    }

    // generate list of output names from output method mappings that are invalid
    if (methodOutputMappings != null) {
      List<String> workflowOutputNames =
          workflowDescription.getOutputs().stream()
              .map(o -> "%s.%s".formatted(workflowName, o.getName()))
              .toList();
      for (MethodOutputMapping outputMapping : methodOutputMappings) {
        if (!workflowOutputNames.contains(outputMapping.getOutputName())) {
          invalidOutputMappings.add(outputMapping.getOutputName());
        }
      }
    }

    if (!invalidInputMappings.isEmpty()) {
      invalidMappingErrors.add(
          "Invalid input mappings. '[%s]' not found in workflow inputs."
              .formatted(String.join(",", invalidInputMappings)));
    }

    if (!invalidOutputMappings.isEmpty()) {
      invalidMappingErrors.add(
          "Invalid output mappings. '[%s]' not found in workflow outputs."
              .formatted(String.join(",", invalidOutputMappings)));
    }

    return invalidMappingErrors;
  }

  private void addLastRunDetails(List<MethodDetails> methodDetails) {
    List<MethodVersionDetails> methodVersionDetails =
        methodDetails.stream()
            .flatMap(
                md ->
                    md.getMethodVersions() == null ? Stream.of() : md.getMethodVersions().stream())
            .toList();

    // Get a set of all run set IDs containing the "last run" information for these methods and
    // versions:
    Set<UUID> lastRunSetIds =
        Stream.concat(
                methodDetails.stream()
                    .flatMap(
                        md ->
                            Boolean.TRUE.equals(md.getLastRun().isPreviouslyRun())
                                ? Stream.of(md.getLastRun().getRunSetId())
                                : Stream.of()),
                methodVersionDetails.stream()
                    .flatMap(
                        mvd ->
                            Boolean.TRUE.equals(mvd.getLastRun().isPreviouslyRun())
                                ? Stream.of(mvd.getLastRun().getRunSetId())
                                : Stream.of()))
            .collect(Collectors.toSet());

    // Fetch the last run details for all run set IDs at the same time:
    var lastRunDetails = methodDao.methodLastRunDetailsFromRunSetIds(lastRunSetIds);

    // Update method details and method version details from the map of last run details:
    for (MethodDetails details : methodDetails) {
      if (Boolean.TRUE.equals(details.getLastRun().isPreviouslyRun())) {
        details.setLastRun(lastRunDetails.get(details.getLastRun().getRunSetId()));
      }
    }

    for (MethodVersionDetails details : methodVersionDetails) {
      if (Boolean.TRUE.equals(details.getLastRun().isPreviouslyRun())) {
        details.setLastRun(lastRunDetails.get(details.getLastRun().getRunSetId()));
      }
    }
  }

  private MethodDetails methodToMethodDetails(Method method, boolean includeVersions) {

    Boolean isMethodPrivate = false;

    if (Objects.equals(method.methodSource(), GITHUB.toString())) {
      GithubMethodDetails details = method.githubMethodDetails().orElse(null);
      if (details != null) {
        isMethodPrivate = details.isPrivate();
      }
    }

    List<MethodVersionDetails> versions =
        includeVersions
            ? methodVersionDao.getMethodVersionsForMethod(method).stream()
                .map(
                    mv -> {
                      try {
                        return methodVersionService.methodVersionToMethodVersionDetails(mv);
                      } catch (MalformedURLException
                          | URISyntaxException
                          | MethodProcessingException
                          | bio.terra.dockstore.client.ApiException e) {
                        log.warn(
                            "methodVersionToMethodVersionDetails conversion failed: %s"
                                .formatted(e));
                        return null;
                      }
                    })
                .toList()
            : null;

    return new MethodDetails()
        .methodId(method.methodId())
        .name(method.name())
        .description(method.description())
        .source(method.methodSource())
        .created(DateUtils.convertToDate(method.created()))
        .lastRun(MethodService.initializeLastRunDetails(method.lastRunSetId()))
        .methodVersions(versions)
        .isPrivate(isMethodPrivate);
  }

  private MethodDetails methodVersionToMethodDetails(MethodVersion methodVersion)
      throws MethodProcessingException, MalformedURLException, URISyntaxException,
          bio.terra.dockstore.client.ApiException {
    Method method = methodVersion.method();
    Boolean isMethodPrivate = false;

    return new MethodDetails()
        .methodId(method.methodId())
        .name(method.name())
        .description(method.description())
        .source(method.methodSource())
        .created(DateUtils.convertToDate(method.created()))
        .lastRun(MethodService.initializeLastRunDetails(method.lastRunSetId()))
        .methodVersions(
            List.of(methodVersionService.methodVersionToMethodVersionDetails(methodVersion)))
        .isPrivate(isMethodPrivate);
  }
}
