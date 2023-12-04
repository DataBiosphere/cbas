package bio.terra.cbas.runsets.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import org.junit.jupiter.api.Test;

class TestFileCounting {

  @Test
  void testFileCountingForFileType() throws CoercionException {
    CbasValue value =
        CbasValue.parseValue(
            "fileInput",
            new ParameterTypeDefinitionPrimitive().primitiveType(PrimitiveParameterValueType.FILE),
            "gs://bucket/dir/file.txt");
    assertEquals(1L, value.countFiles());
  }

  @Test
  void testFileCountingForStringType() throws CoercionException {
    CbasValue value =
        CbasValue.parseValue(
            "fileInput",
            new ParameterTypeDefinitionPrimitive()
                .primitiveType(PrimitiveParameterValueType.STRING),
            "gs://bucket/dir/file.txt");
    assertEquals(0L, value.countFiles());
  }

  @Test
  void testFileCountingForFilledOptionalFileType() throws CoercionException {
    CbasValue value =
        CbasValue.parseValue(
            "fileInput",
            new ParameterTypeDefinitionOptional()
                .optionalType(
                    new ParameterTypeDefinitionPrimitive()
                        .primitiveType(PrimitiveParameterValueType.FILE)),
            "gs://bucket/dir/file.txt");
    assertEquals(1L, value.countFiles());
  }

  @Test
  void testFileCountingForEmptyOptionalFileType() throws CoercionException {
    CbasValue value =
        CbasValue.parseValue(
            "fileInput",
            new ParameterTypeDefinitionOptional()
                .optionalType(
                    new ParameterTypeDefinitionPrimitive()
                        .primitiveType(PrimitiveParameterValueType.FILE)),
            null);
    assertEquals(0L, value.countFiles());
  }
}
