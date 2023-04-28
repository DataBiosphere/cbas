package bio.terra.cbas.runsets.types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.ParameterTypeDefinitionStruct;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import bio.terra.cbas.model.StructField;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestCoercions {

  void testValidStringFileCoercion(String input) throws CoercionException {
    var actual =
        CbasValue.parseValue(
            new ParameterTypeDefinitionPrimitive().primitiveType(PrimitiveParameterValueType.FILE),
            input);
    assertThat(actual, instanceOf(CbasFile.class));
    assertEquals(actual.asSerializableValue(), input);
  }

  @Test
  void testValidLocalStringFileCoercion() throws CoercionException {
    testValidStringFileCoercion("/var/lib/file.txt");
  }

  @Test
  void testValidGcsStringFileCoercion() throws CoercionException {
    testValidStringFileCoercion("gs://bucket/dir/file.txt");
  }

  @Test
  void testValidAzStringFileCoercion() throws CoercionException {
    testValidStringFileCoercion("az://bucket/dir/file.txt");
  }

  @Test
  void testInvalidFileCoercionInputValue() {
    String input = "\"not a file\"";
    Assertions.assertThrows(
        ValueCoercionException.class,
        () ->
            CbasValue.parseValue(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.FILE),
                input));
  }

  @Test
  void testInvalidFileCoercionInputType() {
    Object input = 73;
    Assertions.assertThrows(
        TypeCoercionException.class,
        () ->
            CbasValue.parseValue(
                new ParameterTypeDefinitionPrimitive()
                    .primitiveType(PrimitiveParameterValueType.FILE),
                input));
  }

  @Test
  void autoBoxingStringToArrayStringSucceeds() throws Exception {
    Object input = "String";
    var actual =
        CbasValue.parseValue(
            new ParameterTypeDefinitionArray()
                .arrayType(
                    new ParameterTypeDefinitionPrimitive()
                        .primitiveType(PrimitiveParameterValueType.STRING)
                        .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                .type(ParameterTypeDefinition.TypeEnum.ARRAY),
            input);

    // The result should be a boxed up version of the input:
    assertEquals(actual.asSerializableValue(), List.of(input));
  }

  @Test
  void autoBoxingStringToArrayIntFails() {
    Object input = "String";

    Assertions.assertThrows(
        TypeCoercionException.class,
        () ->
            CbasValue.parseValue(
                new ParameterTypeDefinitionArray()
                    .arrayType(
                        new ParameterTypeDefinitionPrimitive()
                            .primitiveType(PrimitiveParameterValueType.INT)
                            .type(ParameterTypeDefinition.TypeEnum.PRIMITIVE))
                    .type(ParameterTypeDefinition.TypeEnum.ARRAY),
                input));
  }

  @Test
  void testValidOptionalInStruct() throws Exception {
    HashMap<String, String> input = new HashMap<>();
    input.put("foo", "notbar");
    var actual =
        CbasValue.parseValue(
            new ParameterTypeDefinitionStruct()
                .fields(
                    List.of(
                        new StructField()
                            .fieldName("foo")
                            .fieldType(
                                new ParameterTypeDefinitionOptional()
                                    .optionalType(
                                        new ParameterTypeDefinitionPrimitive()
                                            .primitiveType(PrimitiveParameterValueType.STRING))),
                        new StructField()
                            .fieldName("bar")
                            .fieldType(
                                new ParameterTypeDefinitionOptional()
                                    .optionalType(
                                        new ParameterTypeDefinitionPrimitive()
                                            .primitiveType(PrimitiveParameterValueType.STRING)))))
                .type(ParameterTypeDefinition.TypeEnum.STRUCT),
            input);

    assertEquals(input, actual.asSerializableValue());
  }
}
