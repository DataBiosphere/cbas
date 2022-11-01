package bio.terra.cbas.runsets.types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
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
}
