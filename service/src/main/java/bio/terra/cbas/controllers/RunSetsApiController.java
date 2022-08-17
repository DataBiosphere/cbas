package bio.terra.cbas.controllers;

import bio.terra.cbas.api.RunSetsApi;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.model.RunSetRequest;
import bio.terra.cbas.model.RunSetState;
import bio.terra.cbas.model.RunSetStateResponse;
import bio.terra.cbas.model.RunState;
import bio.terra.cbas.model.RunStateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RunSetsApiController implements RunSetsApi {

  private final WdsServerConfiguration wdsConfig;
  private final String workspaceId = "15f36863-30a5-4cab-91f7-52be439f1175";
  private final String wdsApiV = "v0.2";

  private final Gson gson = new GsonBuilder().create();

  @Autowired
  public RunSetsApiController(WdsServerConfiguration wdsConfig) {
    this.wdsConfig = wdsConfig;
  }

  @Override
  public ResponseEntity<RunSetStateResponse> postRunSet(RunSetRequest request) {

    if (request.getWdsEntities().getEntityIds().size() != 1) {
      log.warn("Bad user request: current support is exactly one entity per request");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    String entityType = request.getWdsEntities().getEntityType();
    String entityId = request.getWdsEntities().getEntityIds().get(0);

    HashMap<String, Object> entityAttributes;

    try {
      URL wdsQueryUrl =
          new URL(
              "%s/%s/entities/%s/%s/%s"
                  .formatted(wdsConfig.baseUri(), workspaceId, wdsApiV, entityType, entityId));
      URLConnection connection = wdsQueryUrl.openConnection();
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      try (var stream = connection.getInputStream()) {
        String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        HashMap entityValue = new ObjectMapper().readValue(response, HashMap.class);
        entityAttributes = (HashMap<String, Object>) entityValue.get("attributes");
      }
    } catch (MalformedURLException e) {
      log.warn("Entity lookup failed. Malformed URL", e);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (IOException e) {
      log.warn("Entity lookup failed. Internal IO Exception", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }



    return new ResponseEntity<>(
        new RunSetStateResponse()
            .runSetId(UUID.randomUUID().toString())
            .addRunsItem(
                new RunStateResponse().runId(UUID.randomUUID().toString()).state(RunState.RUNNING))
            .state(RunSetState.RUNNING),
        HttpStatus.OK);
  }
}
