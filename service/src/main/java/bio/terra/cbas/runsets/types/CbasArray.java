package bio.terra.cbas.runsets.types;

import bio.terra.cbas.model.ParameterTypeDefinition;
import java.util.ArrayList;
import java.util.List;

public class CbasArray implements CbasOptional {

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
    return values.stream().map(CbasValue::countFiles).reduce(0L, Long::sum);
  }

  public static CbasArray parseValue(
      ParameterTypeDefinition innerType, Object values, boolean nonEmpty) throws CoercionException {
    if (values instanceof List<?> valueList) {
      List<CbasValue> coercedValues = new ArrayList<>();
      for (Object value : valueList) {
        coercedValues.add(CbasValue.parseValue(innerType, value));
      }
      if (nonEmpty && coercedValues.isEmpty()) {
        throw new ValueCoercionException(
            values,
            "Array[%s]".formatted(innerType),
            "Non-empty array must have at least one value.");
      }

      return new CbasArray(coercedValues);
    } else {
      throw new TypeCoercionException(values, "Array[%s]".formatted(innerType));
    }
  }
}
