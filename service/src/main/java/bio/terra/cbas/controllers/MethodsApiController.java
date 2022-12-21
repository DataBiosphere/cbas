package bio.terra.cbas.controllers;

import bio.terra.cbas.api.MethodsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodLastRunDetails;
import bio.terra.cbas.model.MethodListResponse;
import bio.terra.cbas.model.MethodVersionDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class MethodsApiController implements MethodsApi {
  private final MethodDao methodDao;
  private final MethodVersionDao methodVersionDao;

  public MethodsApiController(MethodDao methodDao, MethodVersionDao methodVersionDao) {
    this.methodDao = methodDao;
    this.methodVersionDao = methodVersionDao;
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

  private void addLastRunDetails(List<MethodDetails> methodDetails) {
    Set<UUID> lastRunSetIds = new HashSet<>();
    for (MethodDetails details : methodDetails) {
      if (details.getLastRun().isPreviouslyRun()) {
        lastRunSetIds.add(details.getLastRun().getRunSetId());
        if (details.getMethodVersions() != null) {
          for (MethodVersionDetails versionDetails : details.getMethodVersions()) {
            if (versionDetails.getLastRun().isPreviouslyRun()) {
              lastRunSetIds.add(versionDetails.getLastRun().getRunSetId());
            }
          }
        }
      }
    }

    if (!lastRunSetIds.isEmpty()) {
      var lastRunDetails = methodDao.methodLastRunDetailsFromRunSetIds(lastRunSetIds);

      for (MethodDetails details : methodDetails) {
        if (details.getLastRun().isPreviouslyRun()) {
          details.setLastRun(lastRunDetails.get(details.getLastRun().getRunSetId()));
          if (details.getMethodVersions() != null) {
            for (MethodVersionDetails versionDetails : details.getMethodVersions()) {
              if (versionDetails.getLastRun().isPreviouslyRun()) {
                versionDetails.setLastRun(
                    lastRunDetails.get(versionDetails.getLastRun().getRunSetId()));
              }
            }
          }
        }
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
        .methodId(method.method_id())
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
        .methodId(methodVersion.method().method_id())
        .name(methodVersion.name())
        .description(methodVersion.description())
        .created(DateUtils.convertToDate(methodVersion.created()))
        .lastRun(initializeLastRunDetails(methodVersion.lastRunSetId()))
        .url(methodVersion.url());
  }

  private MethodDetails methodVersionToMethodDetails(MethodVersion methodVersion) {
    Method method = methodVersion.method();
    return new MethodDetails()
        .methodId(method.method_id())
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
