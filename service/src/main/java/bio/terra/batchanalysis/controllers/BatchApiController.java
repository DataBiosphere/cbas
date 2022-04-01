package bio.terra.batchanalysis.controllers;

import bio.terra.terra_batch_analysis.generated.api.BatchApi;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class BatchApiController implements BatchApi {

  @Override
  public ResponseEntity<List<String>> listMethods() {
    // TODO: Get an answer from the database?
    return ResponseEntity.ok(
        Collections.singletonList(
            "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl"));
  }
}
