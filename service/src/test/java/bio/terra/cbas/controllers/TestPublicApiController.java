package bio.terra.cbas.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.cbas.model.CapabilitiesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  void checkCapabilitiesJsonFileIsValid() {
    assertDoesNotThrow(
        () ->
            objectMapper.readValue(
                capabilitiesResource.getInputStream(), CapabilitiesResponse.class),
        "Resource file 'capabilities.json' is invalid.");
  }

  @Test
  void checkCapabilitiesResponseIsValid() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get(CAPABILITIES_API)).andExpect(status().isOk()).andReturn();

    CapabilitiesResponse parsedResponse =
        assertDoesNotThrow(
            () ->
                objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(), CapabilitiesResponse.class));

    assertThat(parsedResponse).isNotEmpty();
  }
}
