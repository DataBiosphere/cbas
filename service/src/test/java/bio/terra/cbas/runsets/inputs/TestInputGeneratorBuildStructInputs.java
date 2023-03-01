package bio.terra.cbas.runsets.inputs;

import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithOneFieldStructFooRatingParameterObjectBuilder;
import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithOneFieldStructFooRatingParameterRecordLookup;
import static bio.terra.cbas.runsets.inputs.StockInputDefinitions.inputDefinitionWithOneNestedFieldStructFooRatingParameterObjectBuilder;
import static bio.terra.cbas.runsets.inputs.StockWdsRecordResponses.wdsRecordWithFooRating;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.exceptions.InputProcessingException;
import bio.terra.cbas.model.*;
import bio.terra.cbas.runsets.types.CoercionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestInputGeneratorBuildStructInputs {

  @Test
  void workflowInputDefinitionParsingStructs() throws Exception {
    WorkflowInputDefinition actualInputDefinition =
        inputDefinitionWithOneFieldStructFooRatingParameterRecordLookup("struct_field", "Int");

    ParameterTypeDefinition expectedInputTypeDefinition =
        new ParameterTypeDefinitionStruct()
            .name("StructName")
            .fields(
                List.of(
                    new StructField()
                        .fieldName("struct_field")
                        .fieldType(
                            new ParameterTypeDefinitionPrimitive()
                                .primitiveType(PrimitiveParameterValueType.INT)
                                .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))))
            .type(ParameterTypeDefinition.TypeEnum.STRUCT);

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
  void oneFieldStructRecordLookup()
      throws JsonProcessingException, CoercionException, InputProcessingException {

    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                inputDefinitionWithOneFieldStructFooRatingParameterRecordLookup(
                    "struct_field", "Int")),
            wdsRecordWithFooRating("{ \"struct_field\": 5 }"));

    Map<String, Object> expected = Map.of("lookup_foo", Map.of("struct_field", 5L));
    assertEquals(expected, actual);
  }

  @Test
  void oneFieldStructObjectBuilder()
      throws JsonProcessingException, CoercionException, InputProcessingException {

    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                inputDefinitionWithOneFieldStructFooRatingParameterObjectBuilder(
                    "struct_field", "Int")),
            wdsRecordWithFooRating("5"));

    Map<String, Object> expected = Map.of("lookup_foo", Map.of("struct_field", 5L));
    assertEquals(expected, actual);
  }

  @Test
  void nestedOneFieldStructObjectBuilder()
      throws JsonProcessingException, CoercionException, InputProcessingException {

    Map<String, Object> actual =
        InputGenerator.buildInputs(
            List.of(
                inputDefinitionWithOneNestedFieldStructFooRatingParameterObjectBuilder(
                    "struct_field", "inner_struct_field", "Int")),
            wdsRecordWithFooRating("5"));

    Map<String, Object> expected =
        Map.of("lookup_foo", Map.of("struct_field", Map.of("inner_struct_field", 5L)));
    assertEquals(expected, actual);
  }
}
