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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    List<MethodVersionDetails> methodVersionDetails = methodDetails.stream()
        .flatMap(md -> md.getMethodVersions() == null ? Stream.of() : md.getMethodVersions().stream())
        .toList();

    // Get a set of all run set IDs containing the "last run" information for these methods and versions:
    Set<UUID> lastRunSetIds = Stream.concat(
        methodDetails.stream()
            .flatMap(md -> md.getLastRun().isPreviouslyRun() ? Stream.of(md.getLastRun().getRunSetId()) : Stream.of()),
        methodVersionDetails.stream()
            .flatMap(mvd -> mvd.getLastRun().isPreviouslyRun() ? Stream.of(mvd.getLastRun().getRunSetId()) : Stream.of())
    ).collect(Collectors.toSet());

    // Fetch the last run details for all run set IDs at the same time:
    var lastRunDetails = methodDao.methodLastRunDetailsFromRunSetIds(lastRunSetIds);

    // Update method details and method version details from the map of last run details:
    for(MethodDetails details : methodDetails) {
      if (details.getLastRun().isPreviouslyRun()) {
        details.setLastRun(lastRunDetails.get(details.getLastRun().getRunSetId()));
      }
    }

    for (MethodVersionDetails details : methodVersionDetails) {
      if (details.getLastRun().isPreviouslyRun()) {
        details.setLastRun(
            lastRunDetails.get(details.getLastRun().getRunSetId()));
      }
    }
  }

  private Set<UUID> lastRunSetIdsFromMethodVersionDetails(List<MethodVersionDetails> methodVersionDetails) {
    Set<UUID> lastRunSetIds = new HashSet<>();
    for (MethodVersionDetails versionDetails : methodVersionDetails) {
      if (versionDetails.getLastRun().isPreviouslyRun()) {
        lastRunSetIds.add(versionDetails.getLastRun().getRunSetId());
      }
    }
    return lastRunSetIds;
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
