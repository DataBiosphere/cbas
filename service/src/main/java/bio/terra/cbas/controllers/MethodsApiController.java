package bio.terra.cbas.controllers;

import bio.terra.cbas.api.MethodsApi;
import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodListResponse;
import bio.terra.cbas.models.Method;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class MethodsApiController implements MethodsApi {

  private final MethodDao methodDao;

  public MethodsApiController(MethodDao methodDao) {
    this.methodDao = methodDao;
  }

  @Override
  public ResponseEntity<MethodListResponse> getMethods() {
    // TODO: Get an answer from the database?
    return ResponseEntity.ok(
        new MethodListResponse()
            .methods(methodDao.getMethods().stream().map(this::methodToMethodDetails).toList()));
  }

  private MethodDetails methodToMethodDetails(Method method) {
    return new MethodDetails()
        .id(method.method_id().toString())
        .name(method.name())
        .description(method.description())
        .source(method.methodSource())
        .sourceUrl(method.methodSourceUrl())
        .created(DateUtils.convertToDate(method.created()))
        .lastRun(DateUtils.convertToDate(method.lastRun()));
  }
}
