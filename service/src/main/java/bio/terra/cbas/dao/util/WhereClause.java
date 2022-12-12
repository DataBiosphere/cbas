package bio.terra.cbas.dao.util;

import java.util.List;
import java.util.Map;

public record WhereClause(List<String> conditions, Map<String, ?> params) {
  @Override
  public String toString() {
    if (conditions.isEmpty()) {
      return "";
    } else {
      return "WHERE (" + String.join(") AND (", conditions) + ")";
    }
  }
}
