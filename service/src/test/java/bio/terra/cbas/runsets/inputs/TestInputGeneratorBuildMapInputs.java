package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithMapFooRatingParameter;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.model.ParameterDefinition;
import bio.terra.cbas.model.ParameterDefinitionRecordLookup;
import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.WorkflowInputDefinition;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildMapInputs {

  @Test
  void workflowInputDefinitionParsingMaps() throws Exception {
    WorkflowInputDefinition actualInputDefinition =
        inputDefinitionWithMapFooRatingParameter("String", "Int");

    ParameterTypeDefinition expectedInputTypeDefinition =
        new ParameterTypeDefinitionMap()
            .keyType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.STRING)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .valueType(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.INT)
                    .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
            .type(ParameterTypeDefinition.TypeEnum.MAP);

    ParameterDefinition expectedParameterDefinition =
        new ParameterDefinitionRecordLookup()
            .recordAttribute("foo-rating")
            .type(ParameterDefinition.TypeEnum.RECORD_LOOKUP);

    WorkflowInputDefinition expectedInputDefinition =
        new WorkflowInputDefinition()
            .inputName("lookup_foo")
            .inputType(expectedInputTypeDefinition)
            .source(expectedParameterDefinition);

    assertEquals(expectedInputDefinition, actualInputDefinition);
  }

  @Test
  void emptyMapLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {
    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithMapFooRatingParameter("String", "String")),
            wdsRecordWithFooRating("{}"));

    Map<String, Object> expected = Map.of("lookup_foo", Map.of());
    assertEquals(expected, actual);
  }

  @Test
  void oneEntryMapLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {

    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(inputDefinitionWithMapFooRatingParameter("String", "Int")),
            wdsRecordWithFooRating("{ \"five\": 5 }"));

    Map<String, Object> expected = Map.of("lookup_foo", Map.of("five", 5L));
    assertEquals(expected, actual);
  }
}
