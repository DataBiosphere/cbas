package bio.terra.cbas.runsets.types;

import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;

public interface CbasValue {
  Object asSerializableValue();

  /**
   * Count how many Files are in this value.
   *
   * @return count of files
   */
  long countFiles();

  static CbasValue parseValue(ParameterTypeDefinition parameterType, Object value)
      throws CoercionException {
    if (parameterType instanceof ParameterTypeDefinitionPrimitive primitiveDefinition) {
      return switch (primitiveDefinition.getPrimitiveType()) {
        case STRING -> CbasString.parse(value);
        case INT -> CbasInt.parse(value);
        case BOOLEAN -> CbasBoolean.parse(value);
        case FLOAT -> CbasFloat.parse(value);
        case FILE -> CbasFile.parse(value);
      };
    } else if (parameterType instanceof ParameterTypeDefinitionOptional optionalDefinition) {
      if (value == null) {
        return new CbasOptionalNone();
      } else {
        var innerType = optionalDefinition.getOptionalType();
        return new CbasOptionalSome(parseValue(innerType, value));
      }
    } else {
      throw new TypeCoercionException(value, parameterType.toString());
    }
  }
}
