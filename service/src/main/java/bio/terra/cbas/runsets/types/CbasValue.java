package bio.terra.cbas.runsets.types;

import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.PrimitiveParameterValueType;

public interface CbasValue {
  Object asSerializableValue();

  /**
   * Count how many Files are in this value.
   *
   * @return count of files
   */
  long countFiles();

  static CbasValue parsePrimitive(
      PrimitiveParameterValueType primitiveParameterValueType, Object value)
      throws CoercionException {
    return switch (primitiveParameterValueType) {
      case STRING -> CbasString.parse(value);
      case INT -> CbasInt.parse(value);
      case BOOLEAN -> CbasBoolean.parse(value);
      case FLOAT -> CbasFloat.parse(value);
      case FILE -> CbasFile.parse(value);
    };
  }

  static CbasValue parseValue(ParameterTypeDefinition parameterType, Object value)
      throws CoercionException {
    if (parameterType instanceof ParameterTypeDefinitionPrimitive primitiveDefinition) {
      return parsePrimitive(primitiveDefinition.getPrimitiveType(), value);
    } else if (parameterType instanceof ParameterTypeDefinitionOptional optionalDefinition) {
      if (value == null) {
        return new CbasOptionalNone();
      } else {
        var innerType = optionalDefinition.getOptionalType();
        return new CbasOptionalSome(parseValue(innerType, value));
      }
    } else if (parameterType instanceof ParameterTypeDefinitionArray arrayDefinition) {
      var innerType = arrayDefinition.getArrayType();
      return CbasArray.parseValue(innerType, value, arrayDefinition.isNonEmpty());
    } else if (parameterType instanceof ParameterTypeDefinitionMap mapDefinition) {
      PrimitiveParameterValueType keyType = mapDefinition.getKeyType();
      ParameterTypeDefinition valueType = mapDefinition.getValueType();
      return CbasMap.parseValue(keyType, valueType, value);
    } else {
      throw new TypeCoercionException(value, parameterType.toString());
    }
  }
}
