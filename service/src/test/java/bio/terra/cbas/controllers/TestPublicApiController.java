package bio.terra.cbas.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestPublicApiController {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Value("classpath:capabilities.json")
  Resource capabilitiesResource;

  private static final String CAPABILITIES_API = "/capabilities/v1";

  @Test
  void successfullyReturnCapabilities() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get(CAPABILITIES_API)).andExpect(status().isOk()).andReturn();

    Map<String, Object> parsedResponse =
        objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Map.class);

    assertThat(parsedResponse).isNotNull();

    assertEquals(100, parsedResponse.get("submission.limits.maxWorkflows"));
    assertEquals(200, parsedResponse.get("submission.limits.maxInputs"));
    assertEquals(300, parsedResponse.get("submission.limits.maxOutputs"));
    assertEquals(true, parsedResponse.get("workflow.archiving"));
  }

  @Test
  void checkCapabilitiesJsonFileIsValid() {
    assertDoesNotThrow(
        () -> objectMapper.readValue(capabilitiesResource.getInputStream(), Object.class),
        "Resource file 'capabilities.json' is invalid.");
  }
}
