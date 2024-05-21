package bio.terra.cbas.controllers.util;

import static bio.terra.cbas.models.CbasRunStatus.QUEUED;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
  private final RunDao runDao;
  private final RunSetDao runSetDao;

  private final Logger logger = LoggerFactory.getLogger(AsyncExceptionHandler.class);

  private static final String STANDARD_LOG_MESSAGE =
      "Exception thrown in Thread '%s' while executing method '%s'. Error message: %s";

  public AsyncExceptionHandler(RunDao runDao, RunSetDao runSetDao) {
    this.runDao = runDao;
    this.runSetDao = runSetDao;
  }

  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    String methodName = method.getName();
    if (methodName.equals("triggerWorkflowSubmission")) {
      handleExceptionFromAsyncSubmission(ex, methodName, params);
    } else {
      String logMsg =
          STANDARD_LOG_MESSAGE.formatted(
              Thread.currentThread().getName(), method.getName(), ex.getMessage());
      logger.error(logMsg);
    }
  }

  public void handleExceptionFromAsyncSubmission(
      Throwable ex, String methodName, Object... params) {
    RunSet runSet = null;
    RunSetRequest runSetRequest = null;
    var logMsg =
        STANDARD_LOG_MESSAGE.formatted(
            Thread.currentThread().getName(), methodName, ex.getMessage());

    // extract method request parameters
    for (Object o : params) {
      if (o instanceof RunSetRequest) {
        runSetRequest = (RunSetRequest) o;
      }

      if (o instanceof RunSet) {
        runSet = (RunSet) o;
        logMsg =
            "Exception thrown in Thread '%s' while executing method '%s' for Run Set '%s'. Error message: %s"
                .formatted(
                    Thread.currentThread().getName(),
                    methodName,
                    runSet.runSetId(),
                    ex.getMessage());
      }
    }

    logger.error(logMsg);

    // mark Runs and Run Set as needed in Error state
    if (runSet != null) {
      String errorMsg =
          "Something went wrong while submitting workflows. Error: " + ex.getMessage();

      // mark any Runs that are not submitted to Error state
      List<Run> runsInRunSet =
          runDao.getRuns(new RunDao.RunsFilters(runSet.runSetId(), List.of(QUEUED)));
      runsInRunSet.forEach(
          run ->
              runDao.updateRunStatusWithError(
                  run.runId(), CbasRunStatus.SYSTEM_ERROR, DateUtils.currentTimeInUTC(), errorMsg));

      if (runSetRequest != null) {
        int runsCount = runsInRunSet.size();

        // if all the Runs in the Run Set are in Error state, mark the Run Set as in Error state
        // otherwise the status of Run Set will be updated next time it is polled
        if (runSetRequest.getWdsRecords().getRecordIds().size() == runsCount) {
          runSetDao.updateStateAndRunSetDetails(
              runSet.runSetId(),
              CbasRunSetStatus.ERROR,
              runsCount,
              runsCount,
              OffsetDateTime.now());
        }
      }
    }
  }
}
