package bio.terra.cbas.controllers;

import bio.terra.cbas.api.BatchApi;
import cromwell.client.ApiClient;
import cromwell.client.api.WomtoolApi;
import cromwell.client.model.WorkflowDescription;
import java.util.Collections;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class BatchApiController implements BatchApi {

  @Override
  public ResponseEntity<List<String>> listMethods() {
    // TODO: Get an answer from the database?
    return ResponseEntity.ok(
        Collections.singletonList(
            "https://raw.githubusercontent.com/broadinstitute/cromwell/develop/centaur/src/main/resources/standardTestCases/hello/hello.wdl"));
  }
}
