package bio.terra.cbas.runsets.types;

import bio.terra.cbas.model.ParameterTypeDefinitionOptional;
import bio.terra.cbas.model.ParameterTypeDefinitionStruct;
import bio.terra.cbas.model.StructField;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CbasStruct implements CbasValue {

  private final String structName;
  private final Map<String, CbasValue> fields;

  public CbasStruct(String structName, Map<String, CbasValue> fields) {
    this.fields = fields;
    this.structName = structName;
  }

  public String getStructName() {
    return structName;
  }

  public Map<String, CbasValue> getFields() {
    return fields;
  }

  @Override
  public Object asSerializableValue() {
    return fields.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asSerializableValue()));
  }

  @Override
  public long countFiles() {
    return fields.values().stream().mapToLong(CbasValue::countFiles).sum();
  }

  public static CbasStruct parseValue(
      String parameterName, ParameterTypeDefinitionStruct structDefinition, Object values)
      throws CoercionException {
    if (values instanceof Map<?, ?> valueMap) {
      HashMap<String, CbasValue> coercedValues = new HashMap<>();

      for (StructField field : structDefinition.getFields()) {
        if (valueMap.containsKey(field.getFieldName())) {
          coercedValues.put(
              field.getFieldName(),
              CbasValue.parseValue(
                  field.getFieldName(), field.getFieldType(), valueMap.get(field.getFieldName())));
        } else if (field.getFieldType() instanceof ParameterTypeDefinitionOptional) {
          // "do nothing"
        } else {
          throw new ValueCoercionException(
              parameterName,
              values,
              "Struct (%s)".formatted(structDefinition.getName()),
              "Field %s not provided in input for Struct (%s)"
                  .formatted(field.getFieldName(), structDefinition.getName()));
        }
      }
      return new CbasStruct(structDefinition.getName(), coercedValues);
    } else {
      throw new TypeCoercionException(
          parameterName, values, "Struct (%s)".formatted(structDefinition.getName()));
    }
  }
}
