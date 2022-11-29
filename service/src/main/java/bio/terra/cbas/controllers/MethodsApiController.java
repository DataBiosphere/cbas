package bio.terra.cbas.controllers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import bio.terra.cbas.api.MethodsApi;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class MethodsApiController implements MethodsApi {

  @Override
  public ResponseEntity<MethodListResponse> getMethods() {
    // TODO: Get an answer from the database?
    return ResponseEntity.ok(
        new MethodListResponse().methods(new ArrayList<>(List.of(
            new MethodDetails()
                .methodId("fake method id")
                .name("fake method")
                .description("fake method")
                .source("fake method source")
                .sourceUrl("https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl")
                .created(Date.from(Instant.now()))
                .lastRun(Date.from(Instant.now()))
        )))
    );
  }
}
