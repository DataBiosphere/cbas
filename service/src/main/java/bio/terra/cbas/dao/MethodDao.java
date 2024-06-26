package bio.terra.cbas.dao;

import bio.terra.cbas.common.exceptions.MethodNotFoundException;
import bio.terra.cbas.dao.mappers.MethodLastRunDetailsMapper;
import bio.terra.cbas.dao.mappers.MethodMapper;
import bio.terra.cbas.dao.util.SqlPlaceholderMapping;
import bio.terra.cbas.model.MethodLastRunDetails;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MethodDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MethodDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public static final String METHOD_JOIN_GITHUB_METHOD_DETAILS =
      "LEFT JOIN github_method_details on method.%s = github_method_details.%s "
          .formatted(Method.METHOD_ID_COL, GithubMethodDetails.METHOD_ID_COL);

  public Method getMethod(UUID methodId) {
    String sql =
        "SELECT * FROM method %s WHERE method.%s = :methodId AND %s = 'ACTIVE'"
            .formatted(
                METHOD_JOIN_GITHUB_METHOD_DETAILS, Method.METHOD_ID_COL, Method.METHOD_STATUS_COL);
    List<Method> queryResult =
        jdbcTemplate.query(
            sql, new MapSqlParameterSource("methodId", methodId), new MethodMapper());

    if (queryResult.isEmpty()) {
      throw new MethodNotFoundException(methodId);
    } else {
      return queryResult.get(0);
    }
  }

  public List<Method> getMethods() {
    String sql =
        "SELECT * FROM method %s WHERE %s = 'ACTIVE' ORDER BY created DESC"
            .formatted(METHOD_JOIN_GITHUB_METHOD_DETAILS, Method.METHOD_STATUS_COL);
    return jdbcTemplate.query(sql, new MethodMapper());
  }

  public int createMethod(Method method) {
    int createdMethod =
        jdbcTemplate.update(
            "insert into method (method_id, name, description, created, last_run_set_id, method_source, method_original_workspace_id) "
                + "values (:methodId, :name, :description, :created, :lastRunSetId, :methodSource, :originalWorkspaceId)",
            new BeanPropertySqlParameterSource(method));

    if (createdMethod == 1) {
      method
          .githubMethodDetails()
          .ifPresent(
              details ->
                  jdbcTemplate.update(
                      "insert into github_method_details (repository, organization, path, private, method_id) "
                          + "values (:repository, :organization, :path, :isPrivate, :methodId)",
                      new BeanPropertySqlParameterSource(details)));
    }

    return createdMethod;
  }

  public int archiveMethod(UUID methodId) {
    String sql =
        "UPDATE method SET %s = 'ARCHIVED' WHERE %s = :method_id"
            .formatted(Method.METHOD_STATUS_COL, Method.METHOD_ID_COL);
    int result = jdbcTemplate.update(sql, new MapSqlParameterSource(Map.of("method_id", methodId)));
    if (result == 0) {
      throw new MethodNotFoundException(methodId);
    } else {
      return result;
    }
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
                "method_id", runSet.methodVersion().method().methodId())));
  }

  public int unsetLastRunSetId(UUID methodId) {
    String sql =
        "UPDATE method SET %s = NULL WHERE %s = :method_id"
            .formatted(Method.LAST_RUN_SET_ID_COL, Method.METHOD_ID_COL);
    return jdbcTemplate.update(sql, new MapSqlParameterSource(Map.of("method_id", methodId)));
  }

  public int countMethods(String methodName, String methodVersionName) {

    String sql =
        "SELECT COUNT(*) FROM method INNER JOIN method_version "
            + "ON method.method_id = method_version.method_id "
            + "WHERE method.name = :name "
            + "AND method.method_status = 'ACTIVE' "
            + "AND method_version.method_version_name = :methodVersionName ";

    MapSqlParameterSource params =
        new MapSqlParameterSource(
            Map.of(
                "name", methodName,
                "methodVersionName", methodVersionName));

    return jdbcTemplate.queryForObject(sql, params, Integer.class);
  }
}
