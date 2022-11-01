package bio.terra.cbas.runsets.types;

import static bio.terra.cbas.common.MetricsUtil.incrementSuccessfulFileParseCounter;
import static bio.terra.cbas.common.MetricsUtil.incrementUnsuccessfulFileParseCounter;

import java.net.URI;
import java.net.URISyntaxException;

public class CbasFile implements CbasValue {
  private final String value;

  /*
  Private constructor. Use 'parseFile' instead.
   */
  private CbasFile(String value) {
    this.value = value;
  }

  @Override
  public Object asSerializableValue() {
    return value;
  }

  public static CbasFile parse(Object value) throws CoercionException {
    String fromType = value.getClass().getSimpleName();
    String toType = "File";
    if (value instanceof String strValue) {
      try {
        URI uri = new URI(strValue);
        incrementSuccessfulFileParseCounter(uri.getScheme(), fromType);
        return new CbasFile(uri.toString());
      } catch (URISyntaxException e) {
        incrementUnsuccessfulFileParseCounter(fromType);
        throw new ValueCoercionException(fromType, toType, e.getMessage());
      }
    } else if (value instanceof CbasFile fileValue) {
      return new CbasFile(fileValue.value);
    } else {
      throw new TypeCoercionException(value, toType);
    }
  }
}
