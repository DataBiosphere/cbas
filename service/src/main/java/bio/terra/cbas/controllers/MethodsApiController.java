package bio.terra.cbas.controllers;

import bio.terra.cbas.api.MethodsApi;
import bio.terra.cbas.model.MethodDetails;
import bio.terra.cbas.model.MethodListResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class MethodsApiController implements MethodsApi {

  @Override
  public ResponseEntity<MethodListResponse> getMethods() {
    // TODO: Get an answer from the database?
    return ResponseEntity.ok(
        new MethodListResponse()
            .methods(
                new ArrayList<>(
                    List.of(
                        new MethodDetails()
                            .id("fake method id 1")
                            .name("fake method 1")
                            .description("fake method 1")
                            .source("fake method source 1")
                            .sourceUrl(
                                "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl")
                            .created(Date.from(Instant.now()))
                            .lastRun(Date.from(Instant.now())),
                        new MethodDetails()
                            .id("fake method id 2")
                            .name("fake method 2")
                            .description("fake method 2")
                            .source("fake method source 2")
                            .sourceUrl(
                                "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl")
                            .created(Date.from(Instant.now()))
                            .lastRun(Date.from(Instant.now())),
                        new MethodDetails()
                            .id("fake method id 3")
                            .name("fake method 3")
                            .description("fake method 3")
                            .source("fake method source 3")
                            .sourceUrl(
                                "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl")
                            .created(Date.from(Instant.now()))
                            .lastRun(Date.from(Instant.now()))))));
  }
}
