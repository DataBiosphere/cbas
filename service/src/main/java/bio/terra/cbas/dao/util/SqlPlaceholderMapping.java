package bio.terra.cbas.dao.util;

import bio.terra.cbas.util.Pair;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SqlPlaceholderMapping<A> {

  private final String placeholderType;
  private final Collection<A> values;

  private final Map<String, A> placeholderToValueMap;

  private final String sqlPlaceholderList;

  public Map<String, A> getPlaceholderToValueMap() {
    return placeholderToValueMap;
  }

  public String getSqlPlaceholderList() {
    return sqlPlaceholderList;
  }

  public SqlPlaceholderMapping(String placeholderType, Collection<A> values) {
    this.placeholderType = placeholderType;
    this.values = values;
    this.placeholderToValueMap = generatePlaceholderToValueMap(placeholderType, values);
    this.sqlPlaceholderList = generateSqlPlaceholderList(placeholderToValueMap.keySet());
  }

  private Map<String, A> generatePlaceholderToValueMap(
      String placeholderType, Collection<A> values) {
    return Streams.zip(IntStream.range(0, values.size()).boxed(), values.stream(), Pair::new)
        .collect(Collectors.toMap(pair -> "%s_%d".formatted(placeholderType, pair.a()), Pair::b));
  }

  private String generateSqlPlaceholderList(Collection<String> placeholderNames) {
    return placeholderNames.stream().map(n -> ":" + n).collect(Collectors.joining(","));
  }
}
