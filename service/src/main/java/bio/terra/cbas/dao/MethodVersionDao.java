package bio.terra.cbas.dao;

import bio.terra.cbas.dao.mappers.MethodVersionMappers;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
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

  public List<MethodVersion> getMethodVersionsForMethod(Method method) {
    String sql =
        "SELECT * FROM method_version "
            + "INNER JOIN method on method_version.method_id = method.method_id "
            + "WHERE method_version.method_id = :method_id";
    return jdbcTemplate.query(
        sql,
        new MapSqlParameterSource("method_id", method.method_id()),
        new MethodVersionMappers.ShallowMethodVersionMapper(method));
  }

  public int createMethodVersion(MethodVersion methodVersion) {
    return jdbcTemplate.update(
        "insert into method_version (method_version_id, method_id, method_version_name, method_version_description, method_version_created, method_version_last_run_set_id, method_version_url "
            + "values (:methodVersionId, :method_id, :name, :description, :created, :lastRunSetId, :url)",
        new BeanPropertySqlParameterSource(methodVersion));
  }

  public int updateLastRunWithRunSet(RunSet runSet) {
    String sql =
        "UPDATE method_version SET %s = :run_set_id WHERE %s = :method_version_id"
            .formatted(MethodVersion.LAST_RUN_SET_ID_COL, MethodVersion.METHOD_VERSION_ID_COL);

    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                "run_set_id",
                runSet.runSetId(),
                "method_version_id",
                runSet.methodVersion().methodVersionId())));
  }
}
