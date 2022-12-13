package bio.terra.cbas.dao.util;

import bio.terra.cbas.util.Pair;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SqlPlaceholderMapping<A> {

  private final Map<String, A> placeholderToValueMap;

  private final String sqlPlaceholderList;

  public Map<String, A> getPlaceholderToValueMap() {
    return placeholderToValueMap;
  }

  public String getSqlPlaceholderList() {
    return sqlPlaceholderList;
  }

  public SqlPlaceholderMapping(String placeholderType, Collection<A> values) {
    List<String> placeholderNames =
        IntStream.range(0, values.size())
            .mapToObj(i -> "%s_%d".formatted(placeholderType, i))
            .toList();

    this.placeholderToValueMap = generatePlaceholderToValueMap(placeholderNames, values);
    this.sqlPlaceholderList = generateSqlPlaceholderList(placeholderNames);
  }

  private Map<String, A> generatePlaceholderToValueMap(
      List<String> placeholderNames, Collection<A> values) {
    return Streams.zip(placeholderNames.stream(), values.stream(), Pair::new)
        .collect(Collectors.toMap(Pair::a, Pair::b));
  }

  private String generateSqlPlaceholderList(Collection<String> placeholderNames) {
    return placeholderNames.stream().map(n -> ":" + n).collect(Collectors.joining(","));
  }
}
