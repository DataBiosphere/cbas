package bio.terra.cbas.runsets.types;

import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.ParameterTypeDefinitionArray;
import bio.terra.cbas.model.ParameterTypeDefinitionMap;
import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionPrimitive;
import bio.terra.cbas.model.ParameterTypeDefinitionStruct;
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
      String parameterName, PrimitiveParameterValueType primitiveParameterValueType, Object value)
      throws CoercionException {
    return switch (primitiveParameterValueType) {
      case STRING -> CbasString.parse(parameterName, value);
      case INT -> CbasInt.parse(parameterName, value);
      case BOOLEAN -> CbasBoolean.parse(parameterName, value);
      case FLOAT -> CbasFloat.parse(parameterName, value);
      case FILE -> CbasFile.parse(parameterName, value);
    };
  }

  static CbasValue parseValue(
      String parameterName, ParameterTypeDefinition parameterType, Object value)
      throws CoercionException {
    if (parameterType instanceof ParameterTypeDefinitionPrimitive primitiveDefinition) {
      return parsePrimitive(parameterName, primitiveDefinition.getPrimitiveType(), value);
    } else if (parameterType instanceof ParameterTypeDefinitionOptional optionalDefinition) {
      if (value == null) {
        return new CbasOptionalNone();
      } else {
        var innerType = optionalDefinition.getOptionalType();
        return new CbasOptionalSome(parseValue(parameterName, innerType, value));
      }
    } else if (parameterType instanceof ParameterTypeDefinitionArray arrayDefinition) {
      var innerType = arrayDefinition.getArrayType();
      return CbasArray.parseValue(parameterName, innerType, value, arrayDefinition.isNonEmpty());
    } else if (parameterType instanceof ParameterTypeDefinitionMap mapDefinition) {
      PrimitiveParameterValueType keyType = mapDefinition.getKeyType();
      ParameterTypeDefinition valueType = mapDefinition.getValueType();
      return CbasMap.parseValue(parameterName, keyType, valueType, value);
    } else if (parameterType instanceof ParameterTypeDefinitionStruct structDefinition) {
      return CbasStruct.parseValue(parameterName, structDefinition, value);
    } else {
      throw new TypeCoercionException(parameterName, value, parameterType.toString());
    }
  }
}
