package bio.terra.cbas.dao;

import bio.terra.cbas.dao.mappers.MethodLastRunDetailsMapper;
import bio.terra.cbas.dao.mappers.MethodMapper;
import bio.terra.cbas.dao.util.SqlPlaceholderMapping;
import bio.terra.cbas.model.MethodLastRunDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MethodDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MethodDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Method getMethod(UUID methodId) {
    String sql = "SELECT * FROM method WHERE method_id = :method_id";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource("method_id", methodId), new MethodMapper())
        .get(0);
  }

  public List<Method> getMethods() {
    String sql = "SELECT * FROM method GROUP BY method.method_id ORDER BY MIN(created) DESC";
    return jdbcTemplate.query(sql, new MethodMapper());
  }

  public Map<UUID, MethodLastRunDetails> methodLastRunDetailsFromRunSetIds(
      Collection<UUID> runSetIds) {

    if (runSetIds.isEmpty()) {
      return Map.of();
    }

    SqlPlaceholderMapping<UUID> placeholderMapping =
        new SqlPlaceholderMapping<>("runSet", runSetIds);

    String sql =
        "SELECT run_set.%s, run_set.%s, method_version.%s, method_version.%s"
                .formatted(
                    RunSet.SUBMISSION_TIMESTAMP_COL,
                    RunSet.RUN_SET_ID_COL,
                    MethodVersion.METHOD_VERSION_ID_COL,
                    MethodVersion.NAME_COL)
            + " FROM run_set INNER JOIN method_version ON run_set.method_version_id = method_version.method_version_id"
            + " WHERE run_set.%s in (%s) "
                .formatted(RunSet.RUN_SET_ID_COL, placeholderMapping.getSqlPlaceholderList());

    List<MethodLastRunDetails> results =
        jdbcTemplate.query(
            sql,
            new MapSqlParameterSource(placeholderMapping.getPlaceholderToValueMap()),
            new MethodLastRunDetailsMapper());

    return results.stream()
        .collect(Collectors.toMap(MethodLastRunDetails::getRunSetId, Function.identity()));
  }

  public int updateLastRunWithRunSet(RunSet runSet) {
    String sql =
        "UPDATE method SET %s = :run_set_id WHERE %s = :method_id"
            .formatted(Method.LAST_RUN_SET_ID_COL, Method.METHOD_ID_COL);

    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                "run_set_id", runSet.runSetId(),
                "method_id", runSet.methodVersion().method().method_id())));
  }
}
