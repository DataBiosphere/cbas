package bio.terra.cbas.runsets.types;

import bio.terra.cbas.model.ParameterTypeDefinition;
import java.util.ArrayList;
import java.util.List;

public class CbasArray implements CbasValue {

  private final List<CbasValue> values;

  public CbasArray(List<CbasValue> values) {
    this.values = values;
  }

  @Override
  public Object asSerializableValue() {
    return values.stream().map(CbasValue::asSerializableValue).toList();
  }

  @Override
  public long countFiles() {
    return values.stream().mapToLong(CbasValue::countFiles).sum();
  }

  public static CbasArray parseValue(
      String parameterName, ParameterTypeDefinition innerType, Object values, boolean nonEmpty)
      throws CoercionException {
    if (values instanceof List<?> valueList) {
      List<CbasValue> coercedValues = new ArrayList<>();
      for (Object value : valueList) {
        coercedValues.add(CbasValue.parseValue(parameterName, innerType, value));
      }
      if (nonEmpty && coercedValues.isEmpty()) {
        throw new ValueCoercionException(
            parameterName,
            values,
            "Array[%s]".formatted(innerType),
            "Non-empty array must have at least one value.");
      }

      return new CbasArray(coercedValues);
    } else {
      try {
        return new CbasArray(List.of(CbasValue.parseValue(parameterName, innerType, values)));
      } catch (CoercionException e) {
        throw new TypeCoercionException(parameterName, values, "Array[%s]".formatted(innerType));
      }
    }
  }
}
