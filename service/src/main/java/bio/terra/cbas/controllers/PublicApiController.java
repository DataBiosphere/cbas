package bio.terra.cbas.controllers;

import bio.terra.cbas.api.PublicApi;
import bio.terra.cbas.model.SystemStatus;
import bio.terra.cbas.model.SystemStatusSystems;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

@Controller
public class PublicApiController implements PublicApi {

  @Override
  public ResponseEntity<SystemStatus> getStatus() {

    RestTemplate restTemplate = new RestTemplate();
    String x = restTemplate.getForObject("https://www.broadinstitute.org", String.class);

    return new ResponseEntity<>(
        new SystemStatus()
            .ok(true)
            .putSystemsItem(
                "Cromwell",
                new SystemStatusSystems().ok(true).addMessagesItem(x.substring(0, 100))),
        HttpStatus.OK);
  }

  //  private final RestTemplate restTemplate;
  //
  //  public RestService(RestTemplateBuilder restTemplateBuilder) {
  //    this.restTemplate = restTemplateBuilder.build();
  //  }
  //
  //  public String getPostsPlainJSON() {
  //    String url = "https://jsonplaceholder.typicode.com/posts";
  //    return this.restTemplate.getForObject(url, String.class);
  //  }
}
