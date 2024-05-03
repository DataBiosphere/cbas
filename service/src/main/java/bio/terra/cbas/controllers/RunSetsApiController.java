package bio.terra.cbas.controllers;

import static bio.terra.cbas.common.MethodUtil.convertToMethodSourceEnum;
import static bio.terra.cbas.common.MetricsUtil.recordInputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordOutputsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRecordsInRequest;
import static bio.terra.cbas.common.MetricsUtil.recordRunsSubmittedPerRunSet;
import static bio.terra.cbas.model.RunSetState.CANCELING;
import static bio.terra.cbas.models.CbasRunSetStatus.toCbasRunSetApiState;
import static bio.terra.cbas.models.CbasRunStatus.QUEUED;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.common.MethodUtil;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunCreationException;
import bio.terra.cbas.common.exceptions.DatabaseConnectivityException.RunSetCreationException;
import bio.terra.cbas.common.exceptions.ForbiddenException;
import bio.terra.cbas.common.exceptions.MethodProcessingException.UnknownMethodSourceException;
import bio.terra.cbas.config.CbasApiConfiguration;
import bio.terra.cbas.config.CbasContextConfiguration;
import bio.terra.cbas.controllers.util.RunSetsHelper;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.dependencies.dockstore.DockstoreService;
import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.dependencies.wds.WdsService;
import bio.terra.cbas.dependencies.wes.CromwellService;
import bio.terra.cbas.model.AbortRunSetResponse;
import bio.terra.cbas.model.OutputDestination;
import bio.terra.cbas.model.PostMethodRequest;
import bio.terra.cbas.model.RunSetDetailsResponse;
import bio.terra.cbas.model.RunSetListResponse;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.monitoring.TimeLimitedUpdater;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager;
import bio.terra.cbas.runsets.monitoring.RunSetAbortManager.AbortRequestDetails;
import bio.terra.cbas.runsets.monitoring.SmartRunSetsPoller;
import bio.terra.cbas.util.UuidSource;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunSetsApiController implements RunSetsApi {

  private final SamService samService;
  private final CromwellService cromwellService;
  private final WdsService wdsService;
  private final DockstoreService dockstoreService;
  private final MethodVersionDao methodVersionDao;
  private final MethodDao methodDao;
  private final RunSetDao runSetDao;
  private final RunDao runDao;
  private final ObjectMapper objectMapper;
  private final CbasApiConfiguration cbasApiConfiguration;
  private final CbasContextConfiguration cbasContextConfiguration;
  private final SmartRunSetsPoller smartRunSetsPoller;
  private final UuidSource uuidSource;
  private final RunSetAbortManager abortManager;
  private final BearerTokenFactory bearerTokenFactory;
  private final HttpServletRequest httpServletRequest;
  private final RunSetsHelper runSetsHelper;

  public RunSetsApiController(
      SamService samService,
      CromwellService cromwellService,
      WdsService wdsService,
      DockstoreService dockstoreService,
      ObjectMapper objectMapper,
      MethodDao methodDao,
      MethodVersionDao methodVersionDao,
      RunDao runDao,
      RunSetDao runSetDao,
      CbasApiConfiguration cbasApiConfiguration,
      CbasContextConfiguration cbasContextConfiguration,
      SmartRunSetsPoller smartRunSetsPoller,
      UuidSource uuidSource,
      RunSetAbortManager abortManager,
      BearerTokenFactory bearerTokenFactory,
      HttpServletRequest httpServletRequest,
      RunSetsHelper runSetsHelper) {
    this.samService = samService;
    this.cromwellService = cromwellService;
    this.wdsService = wdsService;
    this.dockstoreService = dockstoreService;
    this.objectMapper = objectMapper;
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
    this.runSetDao = runSetDao;
    this.runDao = runDao;
    this.cbasApiConfiguration = cbasApiConfiguration;
    this.cbasContextConfiguration = cbasContextConfiguration;
    this.smartRunSetsPoller = smartRunSetsPoller;
    this.uuidSource = uuidSource;
    this.abortManager = abortManager;
    this.bearerTokenFactory = bearerTokenFactory;
    this.httpServletRequest = httpServletRequest;
    this.runSetsHelper = runSetsHelper;
  }

  private RunSetDetailsResponse convertToRunSetDetails(RunSet runSet) {
    return new RunSetDetailsResponse()
        .runSetId(runSet.runSetId())
        .methodId(runSet.methodVersion().method().methodId())
        .methodVersionId(runSet.methodVersion().methodVersionId())
        .runSetName(runSet.name())
        .runSetDescription(runSet.description())
        .callCachingEnabled(runSet.callCachingEnabled())
        .isTemplate(runSet.isTemplate())
        .state(toCbasRunSetApiState(runSet.status()))
        .recordType(runSet.recordType())
        .submissionTimestamp(DateUtils.convertToDate(runSet.submissionTimestamp()))
        .lastModifiedTimestamp(DateUtils.convertToDate(runSet.lastModifiedTimestamp()))
        .runCount(runSet.runCount())
        .errorCount(runSet.errorCount())
        .inputDefinition(runSet.inputDefinition())
        .outputDefinition(runSet.outputDefinition())
        .userId(runSet.userId());
  }

  @Override
  public ResponseEntity<RunSetListResponse> getRunSets(UUID methodId, Integer pageSize) {
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    if (!samService.hasReadPermission(userToken)) {
      throw new ForbiddenException(SamService.READ_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    RunSetListResponse response;

    List<RunSet> filteredRunSet;

    if (methodId != null) {
      filteredRunSet = Collections.singletonList(runSetDao.getLatestRunSetWithMethodId(methodId));
    } else {
      filteredRunSet = runSetDao.getRunSets(pageSize, false);
    }

    TimeLimitedUpdater.UpdateResult<RunSet> runSetUpdateResult =
        smartRunSetsPoller.updateRunSets(filteredRunSet, userToken);
    List<RunSet> updatedRunSets = runSetUpdateResult.updatedList();
    List<RunSetDetailsResponse> filteredRunSetDetails =
        updatedRunSets.stream().map(this::convertToRunSetDetails).toList();
    response =
        new RunSetListResponse()
            .runSets(filteredRunSetDetails)
            .fullyUpdated(runSetUpdateResult.fullyUpdated());

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    if (!samService.hasWritePermission(userToken)) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    captureRequestMetrics(request);

    // Request validation
    List<String> requestErrors = validateRequest(request, this.cbasApiConfiguration);
    if (!requestErrors.isEmpty()) {
      String errorMsg = "Bad user request. Error(s): " + requestErrors;
      log.warn(errorMsg);
      return new ResponseEntity<>(
          new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST);
    }

    UserStatusInfo user = samService.getSamUser(userToken);

    // Fetch existing method
    MethodVersion methodVersion = methodVersionDao.getMethodVersion(request.getMethodVersionId());

    // Convert method url to raw url and return errors if any. Use the raw url while calling
    // Cromwell's submit workflow endpoint
    String rawMethodUrl;
    try {
      PostMethodRequest.MethodSourceEnum methodSourceEnum =
          convertToMethodSourceEnum(methodVersion.method().methodSource());

      rawMethodUrl =
          MethodUtil.convertToRawUrl(
              methodVersion.url(), methodSourceEnum, methodVersion.name(), dockstoreService);

      // this could happen if there was no url or empty url received in the Dockstore workflow's
      // descriptor response
      if (rawMethodUrl == null || rawMethodUrl.isEmpty()) {
        String errorMsg =
            "Error while retrieving WDL url for Dockstore workflow. No workflow url found specified path.";
        log.warn(errorMsg);
        return new ResponseEntity<>(
            new RunSetStateResponse().errors(errorMsg), HttpStatus.BAD_REQUEST);
      }
    } catch (URISyntaxException
        | MalformedURLException
        | UnknownMethodSourceException
        | bio.terra.dockstore.client.ApiException e) {
      // the flow shouldn't reach here since if it was invalid URL or invalid method source it
      // should have been caught when method was imported
      String errorMsg =
          "Something went wrong while submitting workflow. Error: %s".formatted(e.getMessage());
      log.error(errorMsg, e);
      return new ResponseEntity<>(
          new RunSetStateResponse().errors(errorMsg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // register RunSet
    RunSet runSet;
    try {
      runSet = registerRunSet(request, user, methodVersion);
    } catch (JsonProcessingException | RunSetCreationException e) {
      log.warn("Failed to record run set to database", e);
      return new ResponseEntity<>(
          new RunSetStateResponse()
              .errors("Failed to record run set to database. Error(s): " + e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // create mapping between Record IDs to Run IDs to register runs in database
    Map<String, UUID> recordIdToRunIdMapping =
        request.getWdsRecords().getRecordIds().stream()
            .map(recordId -> Map.entry(recordId, UUID.randomUUID()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // register Runs
    List<RunStateResponse> runStateResponseList;
    try {
      runStateResponseList = registerRunsInRunSet(runSet, recordIdToRunIdMapping);
    } catch (RunCreationException e) {
      String errorMsg =
          "Failed to record runs to database for RunSet %s".formatted(runSet.runSetId());
      log.error(errorMsg, e);

      // before marking RunSet in Error state, ensure that any Runs that were registered in database
      // also get marked as in Error state
      List<Run> runsInRunSet =
          runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), List.of(QUEUED)));
      runsInRunSet.forEach(
          run ->
              runDao.updateRunStatusWithError(
                  run.runId(), CbasRunStatus.SYSTEM_ERROR, DateUtils.currentTimeInUTC(), errorMsg));

      // mark RunSet in Error state
      int runsCount = runsInRunSet.size();
      runSetDao.updateStateAndRunSetDetails(
          runSet.runSetId(), CbasRunSetStatus.ERROR, runsCount, runsCount, OffsetDateTime.now());

      return new ResponseEntity<>(
          new RunSetStateResponse()
              .errors("Failed to record runs to database. Error(s): " + e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    RunSetStateResponse response =
        new RunSetStateResponse()
            .runSetId(runSet.runSetId())
            .runs(runStateResponseList)
            .state(toCbasRunSetApiState(runSet.status()));

    // trigger workflow submission
    runSetsHelper.triggerWorkflowSubmission(
        request, runSet, recordIdToRunIdMapping, userToken, rawMethodUrl);

    // return response
    captureResponseMetrics(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<AbortRunSetResponse> abortRunSet(UUID runSetId) {
    // extract bearer token from request to pass down to API calls
    BearerToken userToken = bearerTokenFactory.from(httpServletRequest);

    if (!samService.hasWritePermission(userToken)) {
      throw new ForbiddenException(SamService.WRITE_ACTION, SamService.RESOURCE_TYPE_WORKSPACE);
    }

    // Get the run set associated with runSetId
    RunSet runSet = runSetDao.getRunSet(runSetId);

    // If RunSet is in Queued state, it means that CBAS is still processing the POST request in the
    // async method. In this case, if the RunSet is set to Cancelling, it might leave the RunSet in
    // weird state as it's possible that the background thread could still be processing Runs
    // when the abort request came in and as a result some Runs that had been submitted to
    // Cromwell got cancelled but some Runs that were still in process of being submitted do in
    // fact get submitted to Cromwell and are started by Cromwell. And hence the RunSet might have
    // some aborted Runs and some Runs in Running state.
    if (runSet.status() == CbasRunSetStatus.QUEUED) {
      String errorMessage =
          "Run Set can't be aborted when it is Queued state as system might still be processing the request.";
      return new ResponseEntity<>(
          new AbortRunSetResponse().runSetId(runSetId).errors(errorMessage), HttpStatus.OK);
    }

    AbortRunSetResponse aborted = new AbortRunSetResponse();

    aborted.runSetId(runSetId);

    AbortRequestDetails abortDetails = abortManager.abortRunSet(runSet, userToken);
    List<String> failedRunIds = abortDetails.getAbortRequestFailedIds();
    List<UUID> submittedAbortWorkflows = abortDetails.getAbortRequestSubmittedIds();

    if (!failedRunIds.isEmpty()) {
      aborted.errors(
          "Run set canceled with errors. Unable to abort workflow(s): %s".formatted(failedRunIds));
    }

    aborted.state(CANCELING);
    aborted.runs(submittedAbortWorkflows);

    return new ResponseEntity<>(aborted, HttpStatus.OK);
  }

  public static void captureRequestMetrics(RunSetRequest request) {
    recordInputsInRequest(request.getWorkflowInputDefinitions().size());
    recordOutputsInRequest(request.getWorkflowOutputDefinitions().size());
    recordRecordsInRequest(request.getWdsRecords().getRecordIds().size());
  }

  public static void captureResponseMetrics(RunSetStateResponse response) {
    long successfulRuns =
        response.getRuns().stream().filter(r -> r.getState() == RunState.UNKNOWN).count();
    recordRunsSubmittedPerRunSet(successfulRuns);
  }

  public static List<String> validateRequest(RunSetRequest request, CbasApiConfiguration config) {
    List<String> errorList = new ArrayList<>();
    errorList.addAll(validateRequestRecordIds(request, config));
    errorList.addAll(validateRequestInputsAndOutputs(request, config));
    return errorList;
  }

  public static List<String> validateRequestRecordIds(
      RunSetRequest request, CbasApiConfiguration config) {
    List<String> errorList = new ArrayList<>();

    // check number of Record IDs in request is within allowed limit
    int recordIdsSize = request.getWdsRecords().getRecordIds().size();
    int recordIdsMax = config.getRunSetsMaximumRecordIds();
    if (recordIdsSize > recordIdsMax) {
      errorList.add(
          "%s record IDs submitted exceeds the maximum value of %s."
              .formatted(recordIdsSize, recordIdsMax));
    }

    // check that there are no duplicated Record IDs present in the request
    List<String> recordIds = request.getWdsRecords().getRecordIds();
    List<String> duplicateRecordIds =
        recordIds.stream().filter(e -> Collections.frequency(recordIds, e) > 1).distinct().toList();
    if (duplicateRecordIds.size() > 0) {
      errorList.add("Duplicate Record ID(s) %s present in request.".formatted(duplicateRecordIds));
    }
    return errorList;
  }

  public static List<String> validateRequestInputsAndOutputs(
      RunSetRequest request, CbasApiConfiguration config) {
    List<String> errorList = new ArrayList<>();

    // check that the number of outputs does not exceed their maximum allowed values
    int numWorkflowInputs = request.getWorkflowInputDefinitions().size();
    int maxWorkflowInputs = config.getMaxWorkflowInputs();
    if (numWorkflowInputs > maxWorkflowInputs) {
      errorList.add(
          "Number of defined inputs (%s) exceeds maximum value (%s)"
              .formatted(numWorkflowInputs, maxWorkflowInputs));
    }

    // check that the number of outputs does not exceed their maximum allowed values
    long numWorkflowOutputs =
        request.getWorkflowOutputDefinitions().stream()
            .filter(
                output ->
                    output.getDestination().getType() == OutputDestination.TypeEnum.RECORD_UPDATE)
            .count();
    int maxWorkflowOutputs = config.getMaxWorkflowOutputs();
    if (numWorkflowOutputs > maxWorkflowOutputs) {
      errorList.add(
          "Number of defined outputs (%s) exceeds maximum value (%s)"
              .formatted(numWorkflowOutputs, maxWorkflowOutputs));
    }

    return errorList;
  }

  private RunSet registerRunSet(
      RunSetRequest runSetRequest, UserStatusInfo user, MethodVersion methodVersion)
      throws JsonProcessingException, RunSetCreationException {
    UUID runSetId = UUID.randomUUID();

    RunSet newRunSet =
        new RunSet(
            runSetId,
            methodVersion,
            runSetRequest.getRunSetName(),
            runSetRequest.getRunSetDescription(),
            runSetRequest.isCallCachingEnabled(),
            false,
            CbasRunSetStatus.QUEUED,
            DateUtils.currentTimeInUTC(),
            DateUtils.currentTimeInUTC(),
            DateUtils.currentTimeInUTC(),
            0,
            0,
            objectMapper.writeValueAsString(runSetRequest.getWorkflowInputDefinitions()),
            objectMapper.writeValueAsString(runSetRequest.getWorkflowOutputDefinitions()),
            runSetRequest.getWdsRecords().getRecordType(),
            user.getUserSubjectId(),
            cbasContextConfiguration.getWorkspaceId());

    int created = runSetDao.createRunSet(newRunSet);

    if (created != 1) {
      throw new RunSetCreationException(runSetRequest.getRunSetName());
    }

    methodDao.updateLastRunWithRunSet(newRunSet);
    methodVersionDao.updateLastRunWithRunSet(newRunSet);

    return newRunSet;
  }

  private List<RunStateResponse> registerRunsInRunSet(
      RunSet runSet, Map<String, UUID> recordIdToRunIdMapping) throws RunCreationException {
    List<RunStateResponse> runStateResponseList = new ArrayList<>();

    for (Map.Entry<String, UUID> entry : recordIdToRunIdMapping.entrySet()) {
      int created =
          runDao.createRun(
              new Run(
                  entry.getValue(),
                  null,
                  runSet,
                  entry.getKey(),
                  DateUtils.currentTimeInUTC(),
                  QUEUED,
                  DateUtils.currentTimeInUTC(),
                  DateUtils.currentTimeInUTC(),
                  null));
      if (created != 1) {
        throw new RunCreationException(runSet.runSetId(), entry.getValue(), entry.getKey());
      }
      runStateResponseList.add(
          new RunStateResponse()
              .runId(entry.getValue())
              .state(CbasRunStatus.toCbasApiState(QUEUED)));
    }

    return runStateResponseList;
  }
}
