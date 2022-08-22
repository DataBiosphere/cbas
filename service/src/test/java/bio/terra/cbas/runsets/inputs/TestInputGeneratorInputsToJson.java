package bio.terra.cbas.runsets.inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorInputsToJson {

  @Test
  void createInputsJsonWithTypes() throws JsonProcessingException {
    Map<String, Object> inputSet =
        Map.of("string_foo", "hello world", "int_foo", 100, "boolean_foo", true, "float_foo", 1.1);

    String jsonified = InputGenerator.inputsToJson(inputSet);

    assertEquals(
        "{\"boolean_foo\":true,\"float_foo\":1.1,\"int_foo\":100,\"string_foo\":\"hello world\"}",
        jsonified);
  }
}
