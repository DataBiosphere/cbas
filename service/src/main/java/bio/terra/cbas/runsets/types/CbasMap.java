package bio.terra.cbas.runsets.types;

import bio.terra.cbas.model.ParameterTypeDefinition;
import bio.terra.cbas.model.PrimitiveParameterValueType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CbasMap implements CbasValue {

  private final Map<CbasValue, CbasValue> values;

  final PrimitiveParameterValueType keyType;
  final ParameterTypeDefinition valueType;

  public CbasMap(
      PrimitiveParameterValueType keyType,
      ParameterTypeDefinition valueType,
      Map<CbasValue, CbasValue> values) {
    this.values = values;
    this.keyType = keyType;
    this.valueType = valueType;
  }

  @Override
  public Object asSerializableValue() {
    return values.entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> e.getKey().asSerializableValue(), e -> e.getValue().asSerializableValue()));
  }

  @Override
  public long countFiles() {
    return values.entrySet().stream()
        .mapToLong(e -> e.getKey().countFiles() + e.getValue().countFiles())
        .sum();
  }

  public static CbasMap parseValue(
      PrimitiveParameterValueType keyType, ParameterTypeDefinition valueType, Object values)
      throws CoercionException {
    if (values instanceof Map<?, ?> valueMap) {
      HashMap<CbasValue, CbasValue> coercedValues = new HashMap<>();
      for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
        coercedValues.put(
            CbasValue.parsePrimitive(keyType, entry.getKey()),
            CbasValue.parseValue(valueType, entry.getValue()));
      }
      return new CbasMap(keyType, valueType, coercedValues);
    } else {
      throw new TypeCoercionException(values, "Map[%s, %s]".formatted(keyType, valueType));
    }
  }
}
