package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MetricsUtil.recordMethodCreationCompletion;

import bio.terra.cbas.api.MethodsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodLastRunDetails;
import bio.terra.cbas.model.MethodListResponse;
import bio.terra.cbas.model.MethodVersionDetails;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.PostMethodResponse;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import cromwell.client.model.WorkflowDescription;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class MethodsApiController implements MethodsApi {
  private final CromwellService cromwellService;
  private final MethodDao methodDao;
  private final MethodVersionDao methodVersionDao;
  private final RunSetDao runSetDao;

  private static final List<String> SUPPORTED_SOURCES = new ArrayList<>(List.of("GitHub"));
  private static final List<String> SUPPORTED_URL_HOSTS =
      new ArrayList<>(List.of("raw.githubusercontent.com"));

  public MethodsApiController(
      CromwellService cromwellService,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      RunSetDao runSetDao) {
    this.cromwellService = cromwellService;
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
    this.runSetDao = runSetDao;
  }

  @Override
  public ResponseEntity<PostMethodResponse> postMethod(PostMethodRequest postMethodRequest) {
    long requestStartNanos = System.nanoTime();

    // validate request
    List<String> validationErrors = validateMethod(postMethodRequest);
    if (!validationErrors.isEmpty()) {
      String errorMsg = "Bad user request. Error(s): " + String.join(". ", validationErrors);
      log.warn(errorMsg);
      return new ResponseEntity<>(new PostMethodResponse().error(errorMsg), HttpStatus.BAD_REQUEST);
    }

    String methodSource = postMethodRequest.getMethodSource().toString();

    // call Cromwell's /describe endpoint to get description of the workflow along with inputs and
    // outputs
    WorkflowDescription workflowDescription;
    try {
      workflowDescription = cromwellService.describeWorkflow(postMethodRequest.getMethodUrl());

      // return 400 if method is invalid
      if (!workflowDescription.getValid()) {
        String invalidMethodErrors =
            String.format(
                "Bad user request. Method '%s' in invalid. Error(s): %s",
                postMethodRequest.getMethodUrl(),
                String.join(". ", workflowDescription.getErrors()));
        log.warn(invalidMethodErrors);
        recordMethodCreationCompletion(
            methodSource, HttpStatus.BAD_REQUEST.value(), requestStartNanos);

        return new ResponseEntity<>(
            new PostMethodResponse().error(invalidMethodErrors), HttpStatus.BAD_REQUEST);
      }

      // store method in database along with input and output definitions
      UUID methodId = UUID.randomUUID();
      UUID methodVersionId = UUID.randomUUID();
      UUID runSetId = UUID.randomUUID();

      // because of FK constraints, lastRunSetId for method and method_version will be added
      // after the run set is created
      Method method =
          new Method(
              methodId,
              postMethodRequest.getMethodName(),
              postMethodRequest.getMethodDescription(),
              DateUtils.currentTimeInUTC(),
              null,
              postMethodRequest.getMethodSource().toString());

      MethodVersion methodVersion =
          new MethodVersion(
              methodVersionId,
              method,
              postMethodRequest.getMethodVersion(),
              method.description(),
              DateUtils.currentTimeInUTC(),
              null,
              postMethodRequest.getMethodUrl());

      String templateRunSetName =
          String.format("%s/%s workflow", method.name(), methodVersion.name());
      String templateRunSetDesc = "Template Run Set for Method " + templateRunSetName;
      RunSet templateRunSet =
          new RunSet(
              runSetId,
              methodVersion,
              templateRunSetName,
              templateRunSetDesc,
              true,
              CbasRunSetStatus.COMPLETE,
              DateUtils.currentTimeInUTC(),
              DateUtils.currentTimeInUTC(),
              DateUtils.currentTimeInUTC(),
              0,
              0,
              "{}", // TODO: https://broadworkbench.atlassian.net/browse/WM-1696
              "{}", // TODO: https://broadworkbench.atlassian.net/browse/WM-1696
              null);

      methodDao.createMethod(method);
      methodVersionDao.createMethodVersion(methodVersion);
      runSetDao.createRunSet(templateRunSet);

      methodDao.updateLastRunWithRunSet(templateRunSet);
      methodVersionDao.updateLastRunWithRunSet(templateRunSet);

      recordMethodCreationCompletion(methodSource, HttpStatus.OK.value(), requestStartNanos);

      PostMethodResponse postMethodResponse =
          new PostMethodResponse().methodId(methodId).runSetId(runSetId);
      return new ResponseEntity<>(postMethodResponse, HttpStatus.OK);
    } catch (cromwell.client.ApiException e) {
      String errorMsg =
          String.format(
              "Something went wrong while importing the method '%s'. Error(s): %s",
              postMethodRequest.getMethodUrl(), e.getMessage());
      log.warn(errorMsg);
      recordMethodCreationCompletion(
          methodSource, HttpStatus.INTERNAL_SERVER_ERROR.value(), requestStartNanos);

      return new ResponseEntity<>(
          new PostMethodResponse().error(errorMsg), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public ResponseEntity<MethodListResponse> getMethods(
      Boolean showVersions, UUID methodId, UUID methodVersionId) {

    List<MethodDetails> methodDetails;

    if (methodVersionId != null) {
      methodDetails =
          List.of(methodVersionToMethodDetails(methodVersionDao.getMethodVersion(methodVersionId)));
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

  public static List<String> validateMethod(PostMethodRequest methodRequest) {
    String methodName = methodRequest.getMethodName();
    String methodSource = methodRequest.getMethodSource().toString();
    String methodVersion = methodRequest.getMethodVersion();
    String methodUrl = methodRequest.getMethodUrl();
    List<String> errors = new ArrayList<>();

    if (methodName == null || methodName.trim().isEmpty()) {
      errors.add("method_name is required");
    }

    if (methodSource == null || methodSource.trim().isEmpty()) {
      errors.add("method_source is required");
    } else {
      // verify that method source is supported. Currently, we only support GitHub as the source
      if (SUPPORTED_SOURCES.stream().noneMatch(methodSource::equalsIgnoreCase)) {
        errors.add("method_source is invalid. Supported source(s): " + SUPPORTED_SOURCES);
      }
    }

    if (methodVersion == null || methodVersion.trim().isEmpty()) {
      errors.add("method_version is required");
    }

    if (methodUrl == null || methodUrl.trim().isEmpty()) {
      errors.add("method_url is required");
    } else {
      // verify that URL is valid, and it's host is supported
      try {
        URL url = new URI(methodUrl).toURL();
        if (!SUPPORTED_URL_HOSTS.contains(url.getHost())) {
          errors.add("method_url is invalid. Supported URI host(s): " + SUPPORTED_URL_HOSTS);
        }
      } catch (URISyntaxException | MalformedURLException e) {
        errors.add("method_url is invalid. Reason: " + e.getMessage());
      }
    }

    return errors;
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

    List<MethodVersionDetails> versions =
        includeVersions
            ? methodVersionDao.getMethodVersionsForMethod(method).stream()
                .map(MethodsApiController::methodVersionToMethodVersionDetails)
                .toList()
            : null;

    return new MethodDetails()
        .methodId(method.methodId())
        .name(method.name())
        .description(method.description())
        .source(method.methodSource())
        .created(DateUtils.convertToDate(method.created()))
        .lastRun(initializeLastRunDetails(method.lastRunSetId()))
        .methodVersions(versions);
  }

  private static MethodVersionDetails methodVersionToMethodVersionDetails(
      MethodVersion methodVersion) {
    return new MethodVersionDetails()
        .methodVersionId(methodVersion.methodVersionId())
        .methodId(methodVersion.method().methodId())
        .name(methodVersion.name())
        .description(methodVersion.description())
        .created(DateUtils.convertToDate(methodVersion.created()))
        .lastRun(initializeLastRunDetails(methodVersion.lastRunSetId()))
        .url(methodVersion.url());
  }

  private MethodDetails methodVersionToMethodDetails(MethodVersion methodVersion) {
    Method method = methodVersion.method();
    return new MethodDetails()
        .methodId(method.methodId())
        .name(method.name())
        .description(method.description())
        .source(method.methodSource())
        .created(DateUtils.convertToDate(method.created()))
        .lastRun(initializeLastRunDetails(method.lastRunSetId()))
        .methodVersions(List.of(methodVersionToMethodVersionDetails(methodVersion)));
  }

  private static MethodLastRunDetails initializeLastRunDetails(UUID lastRunSetId) {
    MethodLastRunDetails lastRunDetails = new MethodLastRunDetails();
    if (lastRunSetId != null) {
      lastRunDetails.setRunSetId(lastRunSetId);
      lastRunDetails.setPreviouslyRun(true);
    } else {
      lastRunDetails.setPreviouslyRun(false);
    }
    return lastRunDetails;
  }
}
