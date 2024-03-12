package bio.terra.cbas.dao;

import bio.terra.cbas.dao.mappers.MethodVersionMappers;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MethodVersionDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MethodVersionDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int createMethodVersion(MethodVersion methodVersion) {
    return jdbcTemplate.update(
        "insert into method_version (method_version_id, method_id, method_version_name, method_version_description, method_version_created, method_version_last_run_set_id, method_version_url, method_version_original_workspace_id, branch_or_tag_name) "
            + "values (:methodVersionId, :methodId, :name, :description, :created, :lastRunSetId, :url, :originalWorkspaceId, :branchOrTagName)",
        new BeanPropertySqlParameterSource(methodVersion));
  }

  public MethodVersion getMethodVersion(UUID methodVersionId) {
    String sql =
        "SELECT * FROM method_version "
            + "INNER JOIN method on method_version.method_id = method.method_id "
            + "WHERE method_version_id = :method_version_id";
    return jdbcTemplate
        .query(
            sql,
            new MapSqlParameterSource("method_version_id", methodVersionId),
            new MethodVersionMappers.DeepMethodVersionMapper())
        .get(0);
  }

  public List<MethodVersion> getMethodVersions() {
    String sql =
        "SELECT * FROM method_version "
            + "INNER JOIN method on method_version.method_id = method.method_id";
    return jdbcTemplate.query(
        sql, new MapSqlParameterSource(), new MethodVersionMappers.DeepMethodVersionMapper());
  }

  public List<MethodVersion> getMethodVersionsForMethod(Method method) {
    String sql =
        "SELECT * FROM method_version "
            + "INNER JOIN method on method_version.method_id = method.method_id "
            + "WHERE method_version.method_id = :methodId";
    return jdbcTemplate.query(
        sql,
        new MapSqlParameterSource("methodId", method.methodId()),
        new MethodVersionMappers.ShallowMethodVersionMapper(method));
  }

  public int updateLastRunSetId(UUID runSetId, UUID methodVersionId) {
    String sql =
        "UPDATE method_version SET %s = :run_set_id WHERE %s = :method_version_id"
            .formatted(MethodVersion.LAST_RUN_SET_ID_COL, MethodVersion.METHOD_VERSION_ID_COL);

    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of("run_set_id", runSetId, "method_version_id", methodVersionId)));
  }

  public int updateOriginalWorkspaceId(UUID methodVersionId, UUID originalWorkspaceId) {
    String updateClause =
        "UPDATE method_version SET %s = :method_version_original_workspace_id"
            .formatted(MethodVersion.ORIGINAL_WORKSPACE_ID_COL);

    HashMap<String, Object> parameterMap =
        new HashMap<>(
            Map.of(
                "method_version_id",
                methodVersionId,
                "method_version_original_workspace_id",
                originalWorkspaceId));

    String sql =
        updateClause
            + " WHERE %s = :method_version_id".formatted(MethodVersion.METHOD_VERSION_ID_COL);

    return jdbcTemplate.update(sql, new MapSqlParameterSource(parameterMap));
  }

  public int deleteMethodVersion(UUID methodVersionId) {
    return jdbcTemplate.update(
        "DELETE FROM method_version WHERE method_version_id = :methodVersionId",
        new MapSqlParameterSource("methodVersionId", methodVersionId));
  }
}
